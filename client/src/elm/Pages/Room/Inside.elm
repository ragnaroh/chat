module Pages.Room.Inside exposing
    ( Model
    , Msg
    , init
    , update
    , view
    , wsSubscriptions
    )

import Browser.Dom
import Context exposing (Context)
import DateTime exposing (DateTime)
import Dict exposing (Dict)
import Form
import Html as H exposing (Html)
import Html.Attributes as HA
import Html.Events as HE
import Html.Keyed
import Json.Decode as JD
import Json.Decode.Pipeline as JDP
import Json.Encode as JE
import Nav
import Pages.Room.Common exposing (RoomId(..), RoomName(..), Username(..))
import Ports
import Task
import WebSocketSub exposing (WebSocketSub)


type alias Model =
    { users : List String
    , events : Dict Int Event
    , messageInput : String
    }


type alias Event =
    { time : DateTime
    , user : String
    , content : EventContent
    }


type EventContent
    = Message String
    | UserEnters
    | UserLeaves


type Msg
    = NoOp
    | SetMessageInput String
    | SendMessage
    | ReceiveTopicMessage (Result JD.Error TopicMessage)
    | ReceiveQueueMessage (Result JD.Error QueueMessage)
    | LeaveRoom


type TopicMessage
    = InitialData ( List String, List ( Int, Event ) )
    | EventTopicMessage ( Int, Event )
    | UsersTopicMessage (List String)


type QueueMessage
    = LeaveRoomQueueMessage



-- INIT


init : ( Model, Cmd Msg )
init =
    ( { users = []
      , events = Dict.empty
      , messageInput = ""
      }
    , Browser.Dom.focus messageInputId |> Task.attempt (always NoOp)
    )



-- UPDATE


update : Msg -> Model -> RoomId -> Context -> ( Model, Cmd Msg )
update msg model roomId context =
    case msg of
        NoOp ->
            ( model, Cmd.none )

        SetMessageInput value ->
            ( { model | messageInput = value }, Cmd.none )

        SendMessage ->
            ( { model | messageInput = "" }
            , case String.trim model.messageInput of
                "" ->
                    Cmd.none

                trimmed ->
                    trimmed
                        |> encodeDataToJs roomId
                        |> Ports.roomMessageOut
            )

        ReceiveTopicMessage (Ok (InitialData ( users, seqEvents ))) ->
            ( { model
                | events = Dict.fromList seqEvents |> Dict.union model.events
                , users = List.sort users
              }
            , jumpToBottom messageHistoryId
            )

        ReceiveTopicMessage (Ok (EventTopicMessage ( seqNum, event ))) ->
            ( { model | events = Dict.insert seqNum event model.events }
            , jumpToBottom messageHistoryId
            )

        ReceiveTopicMessage (Ok (UsersTopicMessage users)) ->
            ( { model | users = List.sort users }
            , Cmd.none
            )

        ReceiveTopicMessage (Err _) ->
            ( model, Cmd.none )

        ReceiveQueueMessage (Ok LeaveRoomQueueMessage) ->
            ( model, Nav.top |> Nav.pushPath context.nav )

        ReceiveQueueMessage (Err _) ->
            ( model, Cmd.none )

        LeaveRoom ->
            ( model
            , Ports.leaveRoom (roomIdToJson roomId)
            )


encodeDataToJs : RoomId -> String -> JE.Value
encodeDataToJs (RoomId roomId) message =
    JE.object
        [ ( "roomId", JE.string roomId )
        , ( "text", JE.string message )
        ]


jumpToBottom : String -> Cmd Msg
jumpToBottom id =
    Browser.Dom.getViewportOf id
        |> Task.andThen (.scene >> .height >> Browser.Dom.setViewportOf id 0)
        |> Task.attempt (always NoOp)


roomIdToJson : RoomId -> JE.Value
roomIdToJson (RoomId roomId) =
    JE.string roomId



-- WEBSOCKET


wsSubscriptions : Model -> RoomId -> WebSocketSub Msg
wsSubscriptions _ roomId =
    WebSocketSub.batch
        [ WebSocketSub.sub
            { endpoint = WebSocketSub.roomTopic roomId
            , msg = ReceiveTopicMessage
            , decoder = topicMessageDecoder
            }
        , WebSocketSub.sub
            { endpoint = WebSocketSub.roomUserQueue roomId
            , msg = ReceiveQueueMessage
            , decoder = queueMessageDecoder
            }
        ]


topicMessageDecoder : JD.Decoder TopicMessage
topicMessageDecoder =
    JD.field "type" JD.string
        |> JD.andThen
            (\messageType ->
                case messageType of
                    "INITIAL_DATA" ->
                        JD.field "object" initialDataDecoder
                            |> JD.map InitialData

                    "EVENT" ->
                        JD.field "object" seqNumAndEventDecoder
                            |> JD.map EventTopicMessage

                    "USERS" ->
                        JD.field "object" (JD.list JD.string)
                            |> JD.map UsersTopicMessage

                    _ ->
                        JD.fail ("Unsupported message type: " ++ messageType)
            )


initialDataDecoder : JD.Decoder ( List String, List ( Int, Event ) )
initialDataDecoder =
    JD.succeed Tuple.pair
        |> JDP.required "users" (JD.list JD.string)
        |> JDP.required "events" (JD.list seqNumAndEventDecoder)


seqNumAndEventDecoder : JD.Decoder ( Int, Event )
seqNumAndEventDecoder =
    JD.succeed Tuple.pair
        |> JDP.required "sequenceNumber" JD.int
        |> JDP.custom eventDecoder


eventDecoder : JD.Decoder Event
eventDecoder =
    JD.succeed Event
        |> JDP.required "timestamp" DateTime.decoder
        |> JDP.required "username" JD.string
        |> JDP.custom (JD.field "type" JD.string |> JD.andThen eventContentDecoder)


eventContentDecoder : String -> JD.Decoder EventContent
eventContentDecoder eventType =
    case eventType of
        "MESSAGE" ->
            JD.field "text" JD.string |> JD.andThen (JD.succeed << Message)

        "JOINED" ->
            JD.succeed UserEnters

        "PARTED" ->
            JD.succeed UserLeaves

        _ ->
            JD.fail ("Unsupported event type: " ++ eventType)


queueMessageDecoder : JD.Decoder QueueMessage
queueMessageDecoder =
    JD.field "type" JD.string
        |> JD.andThen
            (\messageType ->
                case messageType of
                    "LEAVE" ->
                        JD.succeed LeaveRoomQueueMessage

                    _ ->
                        JD.fail ("Unsupported message type: " ++ messageType)
            )



-- VIEW


view : Model -> RoomName -> Context -> List (Html Msg)
view model _ _ =
    [ H.div [ HA.style "height" "100%", HA.style "display" "flex" ]
        [ H.div
            [ HA.class "left-menu is-hidden-mobile"
            , HA.style "flex" "0 1 auto"
            , HA.style "min-width" "9rem"
            , HA.style "padding" "1rem"
            , HA.style "display" "flex"
            , HA.style "flex-direction" "column"
            ]
            [ H.aside
                [ HA.class "menu"
                , HA.style "flex" "1 1 auto"
                ]
                [ H.p [ HA.class "menu-label" ]
                    [ H.text "Users" ]
                , H.ul [ HA.class "menu-list" ]
                    (List.map viewUserItem model.users)
                ]
            , H.div [ HA.style "flex" "0 1 auto" ]
                [ H.button
                    [ HA.class "button is-info is-fullwidth"
                    , HE.onClick LeaveRoom
                    ]
                    [ H.span [ HA.class "icon" ]
                        [ H.i [ HA.class "fas fa-sign-out-alt fa-flip-horizontal" ] [] ]
                    , H.span [] [ H.text "Leave" ]
                    ]
                ]
            ]
        , H.div
            [ HA.style "flex" "1 1 auto"
            , HA.style "display" "flex"
            , HA.style "flex-direction" "column"
            ]
            [ H.div
                [ HA.id messageHistoryId
                , HA.style "flex" "1 1 auto"
                , HA.style "overflow-y" "auto"
                ]
                [ Html.Keyed.ul []
                    (model.events
                        |> Dict.toList
                        |> List.map (\( seqNum, event ) -> viewKeyedEventItem seqNum event)
                    )
                ]
            , H.div
                [ HA.style "flex" "0 1 auto"
                , HA.style "padding" "1rem"
                , Form.onEnter SendMessage
                ]
                [ H.input
                    [ HA.id messageInputId
                    , HA.class "input"
                    , HA.type_ "text"
                    , HE.onInput SetMessageInput
                    , HA.value model.messageInput
                    , HA.autocomplete False
                    ]
                    []
                ]
            ]
        ]
    ]


messageInputId : String
messageInputId =
    "message-input"


messageHistoryId : String
messageHistoryId =
    "message-history"


viewUserItem : String -> Html Msg
viewUserItem username =
    H.li []
        [ H.span []
            [ H.text username ]
        ]


viewKeyedEventItem : Int -> Event -> ( String, Html Msg )
viewKeyedEventItem sequenceNumber event =
    ( String.fromInt sequenceNumber
    , H.li [] [ viewEvent event ]
    )


viewEvent : Event -> Html msg
viewEvent event =
    H.article [ HA.class "media", HA.title (DateTime.hhmmss event.time) ]
        [ H.div [ HA.class "media-content" ]
            [ H.div [ HA.class "content" ]
                [ case event.content of
                    Message text ->
                        viewMessage event.time event.user text

                    UserEnters ->
                        viewPlayerJoin event.time event.user

                    UserLeaves ->
                        viewPlayerPart event.time event.user
                ]
            ]
        ]


viewMessage : DateTime -> String -> String -> Html msg
viewMessage time player text =
    H.p []
        [ H.strong [] [ H.text player ]
        , H.text " "
        , H.small [] [ H.text (DateTime.hhmm time) ]
        , H.br [] []
        , H.text text
        ]


viewPlayerJoin : DateTime -> String -> Html msg
viewPlayerJoin =
    viewPlayerJoinOrPart "joined"


viewPlayerPart : DateTime -> String -> Html msg
viewPlayerPart =
    viewPlayerJoinOrPart "left"


viewPlayerJoinOrPart : String -> DateTime -> String -> Html msg
viewPlayerJoinOrPart joinOrPart time player =
    H.span []
        [ H.i [] [ H.text <| player ++ " " ++ joinOrPart ++ " " ]
        , H.small [] [ H.text (DateTime.hhmm time) ]
        ]
