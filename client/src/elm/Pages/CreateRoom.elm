module Pages.CreateRoom exposing
    ( Model
    , Msg
    , Route
    , init
    , subscriptions
    , update
    , urlParser
    , view
    , wsSubscriptions
    )

import Api
import Browser.Dom
import Browser.Navigation as Nav
import Context exposing (Context)
import Form
import Html as H exposing (Html)
import Html.Attributes as HA
import Html.Events as HE
import Http
import Json.Decode as JD
import Navigation
import Pages.Room.Common exposing (RoomId(..))
import Task
import Url.Parser
import WebSocketSub exposing (WebSocketSub)


type alias Model =
    { roomNameInput : String
    }


type Msg
    = NoOp
    | SetRoomNameInput String
    | CreateRoom
    | ReceiveRegisterRoomResponse (Result Http.Error RoomId)


type alias Route =
    ()



-- INIT


init : Route -> Context -> ( Model, Cmd Msg )
init _ _ =
    ( { roomNameInput = "" }
    , Browser.Dom.focus roomNameInputId |> Task.attempt (always NoOp)
    )



-- UPDATE


update : Msg -> Model -> Context -> ( Model, Cmd Msg )
update msg model context =
    case msg of
        NoOp ->
            ( model, Cmd.none )

        SetRoomNameInput value ->
            ( { model | roomNameInput = value }
            , Cmd.none
            )

        CreateRoom ->
            -- TODO: validate room name
            ( { model | roomNameInput = String.trim model.roomNameInput }
            , createRoomCmd model.roomNameInput context
            )

        ReceiveRegisterRoomResponse (Ok roomId) ->
            ( model
            , Navigation.room roomId |> Navigation.pushUrl context
            )

        ReceiveRegisterRoomResponse (Err _) ->
            ( model, Cmd.none )


createRoomCmd : String -> Context -> Cmd Msg
createRoomCmd name context =
    Api.request
        { endpoint = Api.createRoom name
        , context = context
        , msg = ReceiveRegisterRoomResponse
        , decoder = JD.string |> JD.map RoomId
        }



-- ROUTES


urlParser : Url.Parser.Parser (Route -> a) a
urlParser =
    Url.Parser.map () Url.Parser.top



-- SUBSCRIPTIONS


subscriptions : Model -> Context -> Sub Msg
subscriptions _ _ =
    Sub.none



-- WEBSOCKET


wsSubscriptions : Model -> Context -> WebSocketSub Msg
wsSubscriptions _ _ =
    WebSocketSub.none



-- VIEW


view : Model -> Context -> List (Html Msg)
view model _ =
    [ H.div [ HA.class "hero is-info has-text-centered" ]
        [ H.div [ HA.class "hero-body" ]
            [ H.div [ HA.class "container" ]
                [ H.p [ HA.class "title" ]
                    [ H.text "Create a new room" ]
                ]
            ]
        ]
    , H.section [ HA.class "section" ]
        [ H.div [ HA.class "columns is-centered" ]
            [ H.div
                [ HA.class "column is-two-thirds-tablet is-half-desktop is-two-fifths-widescreen is-one-third-fullhd"
                ]
                [ H.div [ HA.class "field has-addons" ] <|
                    [ H.div [ HA.class "control is-expanded" ]
                        [ H.input
                            [ HA.id roomNameInputId
                            , HA.class "input"
                            , HA.type_ "text"
                            , HA.placeholder "Room name"
                            , HE.onInput SetRoomNameInput
                            , HA.value model.roomNameInput
                            , Form.onEnter CreateRoom
                            ]
                            []
                        ]
                    , H.div [ HA.class "control" ]
                        [ H.button [ HA.class "button is-info", HE.onClick CreateRoom ]
                            [ H.text "Create" ]
                        ]
                    ]
                ]
            ]
        ]
    ]


roomNameInputId : String
roomNameInputId =
    "room-name-input"
