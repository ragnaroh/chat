module Pages.Home exposing
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

import Context exposing (Context)
import Html as H exposing (Html)
import Html.Attributes as HA
import Url.Parser
import WebSocketSub exposing (WebSocketSub)


type alias Model =
    ()


type alias Msg =
    ()


type alias Route =
    ()



-- INIT


init : Route -> Context -> ( Model, Cmd Msg )
init _ _ =
    ( (), Cmd.none )



-- ROUTES


urlParser : Url.Parser.Parser (Route -> a) a
urlParser =
    Url.Parser.map () Url.Parser.top



-- UPDATE


update : Msg -> Model -> Context -> ( Model, Cmd Msg )
update _ model _ =
    ( model, Cmd.none )



-- SUBSCRIPTIONS


subscriptions : Model -> Context -> Sub Msg
subscriptions _ _ =
    Sub.none



-- WEBSOCKET


wsSubscriptions : Model -> Context -> WebSocketSub Msg
wsSubscriptions _ _ =
    WebSocketSub.none



-- VIEW


view : Model -> Context -> List (Html msg)
view _ context =
    [ H.div [ HA.class "hero is-info has-text-centered" ]
        [ H.div [ HA.class "hero-body" ]
            [ H.div [ HA.class "container" ]
                [ H.p [ HA.class "title" ]
                    [ H.text "Welcome!" ]
                ]
            ]
        ]
    , H.section [ HA.class "section" ]
        [ H.div [ HA.class "columns is-centered" ]
            [ viewButtonColumn (context.appPath ++ "/create") "Create room"
            , viewButtonColumn (context.appPath ++ "/join") "Join room"
            ]
        ]
    ]


viewButtonColumn : String -> String -> Html msg
viewButtonColumn href text =
    H.div
        [ HA.class "column is-one-third-tablet is-one-quarter-desktop is-one-fifth-widescreen" ]
        [ H.a [ HA.class "button is-large is-info is-fullwidth", HA.href href ]
            [ H.text text ]
        ]
