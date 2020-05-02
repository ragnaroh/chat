module Pages.JoinRoom exposing
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
import Context exposing (Context)
import Html as H exposing (Html)
import Html.Attributes as HA
import Http
import Json.Decode as JD
import Json.Decode.Pipeline as JDP
import Url.Parser
import WebSocketSub exposing (WebSocketSub)



-- MODEL


type alias Model =
    { rooms : Maybe (Result Http.Error (List Room)) }


type alias Room =
    { id : String
    , name : String
    , users : Int
    }


type Msg
    = ReceiveRooms (Result Http.Error (List Room))


type alias Route =
    ()



-- INIT


init : Route -> Context -> ( Model, Cmd Msg )
init _ context =
    ( Model Nothing, fetchRoomsCmd context )


fetchRoomsCmd : Context -> Cmd Msg
fetchRoomsCmd context =
    Api.request
        { endpoint = Api.getRooms
        , context = context
        , msg = ReceiveRooms
        , decoder = JD.list roomDecoder
        }


roomDecoder : JD.Decoder Room
roomDecoder =
    JD.succeed Room
        |> JDP.required "id" JD.string
        |> JDP.required "name" JD.string
        |> JDP.required "users" JD.int



-- ROUTES


urlParser : Url.Parser.Parser (Route -> a) a
urlParser =
    Url.Parser.map () Url.Parser.top



-- UPDATE


update : Msg -> Model -> Context -> ( Model, Cmd Msg )
update msg _ _ =
    case msg of
        ReceiveRooms result ->
            ( Model (Just result)
            , Cmd.none
            )



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
view model context =
    [ H.div [ HA.class "hero is-info has-text-centered" ]
        [ H.div [ HA.class "hero-body" ]
            [ H.div [ HA.class "container" ]
                [ H.p [ HA.class "title" ]
                    [ H.text "Join an existing room" ]
                ]
            ]
        ]
    , H.section [ HA.class "section" ]
        [ H.div [ HA.class "columns is-centered" ]
            [ H.div
                [ HA.class "column is-two-thirds-tablet is-half-desktop is-two-fifths-widescreen is-one-third-fullhd" ]
                [ viewRoomsPanel context model.rooms
                ]
            ]
        ]
    ]


viewRoomsPanel : Context -> Maybe (Result Http.Error (List Room)) -> Html msg
viewRoomsPanel context maybeRooms =
    H.nav [ HA.class "panel is-info" ]
        [ H.p [ HA.class "panel-heading" ]
            [ H.text "Rooms" ]
        , H.div [ HA.style "max-height" "500px", HA.style "overflow-y" "auto" ] <|
            case maybeRooms of
                Just (Ok rooms) ->
                    viewRoomPanelBlocks context rooms

                Just (Err _) ->
                    [ H.div [ HA.class "column" ]
                        [ H.text "Could not load rooms" ]
                    ]

                Nothing ->
                    [ H.div [ HA.class "column has-text-centered" ]
                        [ H.span [ HA.class "icon" ]
                            [ H.i [ HA.class "fas fa-spinner fa-spin" ] [] ]
                        ]
                    ]
        ]


viewRoomPanelBlocks : Context -> List Room -> List (Html msg)
viewRoomPanelBlocks context rooms =
    if List.isEmpty rooms then
        [ H.div [ HA.class "column" ]
            [ H.text "There are no existing rooms" ]
        ]

    else
        List.map (viewRoomPanelBlock context) rooms


viewRoomPanelBlock : Context -> Room -> Html msg
viewRoomPanelBlock context room =
    H.a [ HA.class "panel-block", HA.href (context.appPath ++ "/room/" ++ room.id) ]
        [ H.div [ HA.class "column" ]
            [ H.span [ HA.class "is-pulled-left" ]
                [ H.text room.name ]
            , H.span [ HA.class "is-pulled-right" ]
                [ H.span [ HA.class "icon", HA.style "margin-right" "0.5em" ]
                    [ H.i [ HA.class "fas fa-users" ] [] ]
                , H.text (String.fromInt room.users)
                ]
            ]
        ]
