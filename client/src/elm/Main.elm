module Main exposing (main)

import Api
import Browser
import Browser.Navigation
import Context exposing (Context)
import Html as H
import Json.Decode as JD
import Json.Decode.Pipeline as JDP
import Navigation
import Pages
import Ports
import Task
import Url
import WebSocketSub


main : Program JD.Value Model Msg
main =
    Browser.application
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        , onUrlChange = UrlChanged
        , onUrlRequest = LinkClicked
        }


type Model
    = InvalidFlags
    | Model MainModel


type alias MainModel =
    { context : Context
    , connected : Bool
    , pagesModel : Pages.Model
    }


type Msg
    = NoOp
    | LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | PagesMsg Pages.Msg
    | UpdateConnectionStatus Bool
    | ReceivedWebSocketMessage String JD.Value



-- INIT


init : JD.Value -> Url.Url -> Browser.Navigation.Key -> ( Model, Cmd Msg )
init flags url navKey =
    case JD.decodeValue flagsDecoder flags of
        Ok { appPath, apiPath } ->
            let
                navContext =
                    Navigation.context appPath navKey

                apiContext =
                    Api.context apiPath

                context =
                    Context navContext apiContext

                route =
                    Pages.decodeUrl url context

                ( pagesModel, cmd ) =
                    Pages.init route context
            in
            ( Model
                { context = context
                , connected = False
                , pagesModel = pagesModel
                }
            , Cmd.map PagesMsg cmd
            )

        Err _ ->
            ( InvalidFlags, Cmd.none )


type alias Flags =
    { appPath : String
    , apiPath : String
    }


flagsDecoder : JD.Decoder Flags
flagsDecoder =
    JD.succeed Flags
        |> JDP.required "appPath" JD.string
        |> JDP.required "apiPath" JD.string



-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case model of
        InvalidFlags ->
            ( model, Cmd.none )

        Model mainModel ->
            let
                ( newModel, cmd ) =
                    updateMain msg mainModel
            in
            ( Model newModel
            , Cmd.batch
                [ cmd
                , if newModel.connected then
                    refreshWebSocktSubscriptionsCmd newModel.pagesModel newModel.context

                  else
                    Cmd.none
                ]
            )


updateMain : Msg -> MainModel -> ( MainModel, Cmd Msg )
updateMain msg model =
    case msg of
        NoOp ->
            ( model, Cmd.none )

        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    ( model, Navigation.pushUrl model.context.nav url )

                Browser.External href ->
                    ( model, Browser.Navigation.load href )

        UrlChanged url ->
            let
                route =
                    Pages.decodeUrl url model.context
            in
            Pages.init route model.context
                |> Tuple.mapFirst (\newModel -> { model | pagesModel = newModel })
                |> Tuple.mapSecond (Cmd.map PagesMsg)

        PagesMsg subMsg ->
            let
                ( newModel, cmd ) =
                    Pages.update subMsg model.pagesModel model.context
            in
            ( { model | pagesModel = newModel }
            , Cmd.map PagesMsg cmd
            )

        UpdateConnectionStatus connected ->
            ( { model | connected = connected }
            , if connected then
                Cmd.none

              else
                Navigation.top |> Navigation.loadPath model.context.nav
            )

        ReceivedWebSocketMessage endpoint payload ->
            ( model
            , Pages.wsSubscriptions model.pagesModel model.context
                |> WebSocketSub.msgs endpoint payload
                |> List.map toCmd
                |> List.map (Cmd.map PagesMsg)
                |> Cmd.batch
            )


refreshWebSocktSubscriptionsCmd : Pages.Model -> Context -> Cmd Msg
refreshWebSocktSubscriptionsCmd pagesModel context =
    Pages.wsSubscriptions pagesModel context
        |> WebSocketSub.endpoints
        |> Ports.refreshWebSocketSubscriptions


toCmd : msg -> Cmd msg
toCmd msg =
    Task.perform (always msg) (Task.succeed ())



-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "Chat"
    , body =
        case model of
            InvalidFlags ->
                [ H.text "An error occurred" ]

            Model { pagesModel, context } ->
                Pages.view pagesModel context
                    |> List.map (H.map PagesMsg)
    }



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
    case model of
        InvalidFlags ->
            Sub.none

        Model { pagesModel, context } ->
            Sub.batch
                [ Pages.subscriptions pagesModel context
                    |> Sub.map PagesMsg
                , Ports.connectionStatusIn connectionStatusInputToMsg
                , Ports.webSocketMessageIn webSocketInputToMsg
                ]


connectionStatusInputToMsg : JD.Value -> Msg
connectionStatusInputToMsg value =
    JD.decodeValue JD.bool value
        |> Result.map UpdateConnectionStatus
        |> Result.withDefault NoOp


webSocketInputToMsg : JD.Value -> Msg
webSocketInputToMsg value =
    let
        decoder =
            JD.succeed Tuple.pair
                |> JDP.required "endpoint" JD.string
                |> JDP.required "payload" JD.value
    in
    case JD.decodeValue decoder value of
        Ok ( endpoint, payload ) ->
            ReceivedWebSocketMessage endpoint payload

        Err _ ->
            NoOp
