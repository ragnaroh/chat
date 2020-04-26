module Main exposing (main)

import Browser
import Browser.Navigation
import Context exposing (Context)
import Html as H
import Json.Decode as JD
import Json.Decode.Pipeline as JDP
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
    , pagesModel : Pages.Model
    }


type Msg
    = NoOp
    | LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | PagesMsg Pages.Msg
    | ReceivedWebSocketMessage String JD.Value



-- INIT


init : JD.Value -> Url.Url -> Browser.Navigation.Key -> ( Model, Cmd Msg )
init flags url navKey =
    case JD.decodeValue flagsDecoder flags of
        Ok { appPath, apiPath } ->
            let
                context =
                    Context appPath apiPath navKey

                route =
                    Pages.decodeUrl url context

                ( pagesModel, cmd ) =
                    Pages.init route context
            in
            ( Model
                { context = context
                , pagesModel = pagesModel
                }
            , Cmd.batch
                [ Cmd.map PagesMsg cmd
                , refreshWebSocktSubscriptionsCmd pagesModel context
                ]
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
                , refreshWebSocktSubscriptionsCmd newModel.pagesModel newModel.context
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
                    ( model, Browser.Navigation.pushUrl model.context.navKey (Url.toString url) )

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

        ReceivedWebSocketMessage destination payload ->
            ( model
            , Pages.wsSubscriptions model.pagesModel model.context
                |> WebSocketSub.msgs destination payload
                |> List.map toCmd
                |> List.map (Cmd.map PagesMsg)
                |> Cmd.batch
            )


refreshWebSocktSubscriptionsCmd : Pages.Model -> Context -> Cmd Msg
refreshWebSocktSubscriptionsCmd pagesModel context =
    Pages.wsSubscriptions pagesModel context
        |> WebSocketSub.destinations
        |> Ports.refreshWebSocketSubscriptions


toCmd : msg -> Cmd msg
toCmd msg =
    Task.perform (always msg) (Task.succeed ())



-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "Chat "
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
                , Ports.webSocketMessageIn webSocketInputToMsg
                ]


webSocketInputToMsg : JD.Value -> Msg
webSocketInputToMsg value =
    let
        decoder =
            JD.succeed Tuple.pair
                |> JDP.required "destination" JD.string
                |> JDP.required "payload" JD.value
    in
    case JD.decodeValue decoder value of
        Ok ( destination, payload ) ->
            ReceivedWebSocketMessage destination payload

        Err _ ->
            NoOp
