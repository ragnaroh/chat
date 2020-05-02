module Pages.Room.Lobby exposing
    ( Model
    , Msg
    , Route
    , UpdateResult(..)
    , init
    , update
    , view
    )

import Api
import Browser.Dom
import Context exposing (Context)
import Form
import Html as H exposing (Html)
import Html.Attributes as HA
import Html.Events as HE
import Http
import Json.Decode as JD
import Pages.Room.Common exposing (RoomId(..), RoomName(..), Username(..))
import Task


type alias Model =
    { usernameInput : String
    , usernameTaken : Bool
    }


type Msg
    = NoOp
    | SetUsernameInput String
    | CreateEnterRoomRequest
    | ReceiveEnterRoomResponse (Result Http.Error Bool)


type alias Route =
    String


type UpdateResult
    = EnterRoom
    | ModelAndCmd Model (Cmd Msg)
    | None



-- INIT


init : ( Model, Cmd Msg )
init =
    ( Model "" False
    , Browser.Dom.focus usernameInputId |> Task.attempt (always NoOp)
    )



-- UPDATE


update : Msg -> Model -> RoomId -> Context -> UpdateResult
update msg model roomId context =
    case msg of
        NoOp ->
            None

        SetUsernameInput value ->
            ModelAndCmd { model | usernameInput = value } Cmd.none

        CreateEnterRoomRequest ->
            -- TODO: validate username
            ModelAndCmd { model | usernameTaken = False }
                (enterRoomCmd roomId model.usernameInput context)

        ReceiveEnterRoomResponse (Ok True) ->
            EnterRoom

        ReceiveEnterRoomResponse (Ok False) ->
            ModelAndCmd { model | usernameTaken = True } Cmd.none

        ReceiveEnterRoomResponse (Err _) ->
            None


enterRoomCmd : RoomId -> String -> Context -> Cmd Msg
enterRoomCmd roomId username context =
    Api.request
        { endpoint = Api.enterRoom roomId username
        , context = context
        , msg = ReceiveEnterRoomResponse
        , decoder = JD.bool
        }



-- VIEW


view : Model -> RoomName -> Context -> List (Html Msg)
view model (RoomName roomName) _ =
    [ H.div [ HA.class "hero is-info has-text-centered" ]
        [ H.div [ HA.class "hero-body" ]
            [ H.div [ HA.class "container" ]
                [ H.p [ HA.class "title" ]
                    [ H.text <| "Enter " ++ roomName ]
                ]
            ]
        ]
    , H.section [ HA.class "section" ]
        [ H.div [ HA.class "columns is-centered is-vcentered" ]
            [ H.div
                [ HA.class "column is-two-thirds-tablet is-half-desktop is-two-fifths-widescreen is-one-third-fullhd"
                , Form.onEnter CreateEnterRoomRequest
                ]
                [ H.div [ HA.class "field has-addons" ] <|
                    [ H.div [ HA.class "control is-expanded" ] <|
                        [ H.input
                            [ HA.id usernameInputId
                            , HA.classList [ ( "input", True ), ( "is-danger", model.usernameTaken ) ]
                            , HA.type_ "text"
                            , HA.placeholder "Username"
                            , HE.onInput SetUsernameInput
                            , HA.value model.usernameInput
                            ]
                            []
                        ]
                            ++ (if model.usernameTaken then
                                    [ H.p [ HA.class "help is-danger" ] [ H.text "The username is taken" ] ]

                                else
                                    []
                               )
                    , H.div [ HA.class "control" ]
                        [ H.button [ HA.class "button is-info", HE.onClick CreateEnterRoomRequest ]
                            [ H.text "Enter" ]
                        ]
                    ]
                ]
            ]
        ]
    ]


usernameInputId : String
usernameInputId =
    "username-input"
