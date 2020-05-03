module Pages exposing
    ( Model
    , Msg
    , Route
    , decodeUrl
    , init
    , subscriptions
    , update
    , view
    , wsSubscriptions
    )

import Context exposing (Context)
import Html as H exposing (Html)
import Nav
import Pages.CreateRoom as CreateRoom
import Pages.Home as Home
import Pages.JoinRoom as JoinRoom
import Pages.Room as Room
import Url exposing (Url)
import Url.Parser exposing ((</>))
import WebSocketSub exposing (WebSocketSub)



-- MODEL


type Model
    = HomeModel Home.Model
    | CreateRoomModel CreateRoom.Model
    | JoinRoomModel JoinRoom.Model
    | RoomModel Room.Model
    | NotFound


type Msg
    = HomeMsg Home.Msg
    | CreateRoomMsg CreateRoom.Msg
    | JoinRoomMsg JoinRoom.Msg
    | RoomMsg Room.Msg


type Route
    = HomeRoute Home.Route
    | CreateRoomRoute CreateRoom.Route
    | JoinRoomRoute JoinRoom.Route
    | RoomRoute Room.Route
    | InvalidRoute



-- INIT


init : Route -> Context -> ( Model, Cmd Msg )
init route context =
    case route of
        HomeRoute subRoute ->
            Home.init subRoute context
                |> lift HomeModel HomeMsg

        CreateRoomRoute subRoute ->
            CreateRoom.init subRoute context
                |> lift CreateRoomModel CreateRoomMsg

        JoinRoomRoute subRoute ->
            JoinRoom.init subRoute context
                |> lift JoinRoomModel JoinRoomMsg

        RoomRoute subRoute ->
            Room.init subRoute context
                |> lift RoomModel RoomMsg

        InvalidRoute ->
            ( NotFound, Cmd.none )



-- UPDATE


update : Msg -> Model -> Context -> ( Model, Cmd Msg )
update msg model context =
    case ( msg, model ) of
        ( HomeMsg subMsg, HomeModel subModel ) ->
            Home.update subMsg subModel context
                |> lift HomeModel HomeMsg

        ( CreateRoomMsg subMsg, CreateRoomModel subModel ) ->
            CreateRoom.update subMsg subModel context
                |> lift CreateRoomModel CreateRoomMsg

        ( JoinRoomMsg subMsg, JoinRoomModel subModel ) ->
            JoinRoom.update subMsg subModel context
                |> lift JoinRoomModel JoinRoomMsg

        ( RoomMsg subMsg, RoomModel subModel ) ->
            Room.update subMsg subModel context
                |> lift RoomModel RoomMsg

        ( _, _ ) ->
            ( model, Cmd.none )



-- ROUTES


decodeUrl : Url -> Context -> Route
decodeUrl url context =
    Url.Parser.parse (Nav.baseUrlParser context.nav </> urlParser) url
        |> Maybe.withDefault InvalidRoute


urlParser : Url.Parser.Parser (Route -> a) a
urlParser =
    Url.Parser.oneOf
        [ Url.Parser.map HomeRoute (Url.Parser.top </> Home.urlParser)
        , Url.Parser.map CreateRoomRoute (Url.Parser.s "create" </> CreateRoom.urlParser)
        , Url.Parser.map JoinRoomRoute (Url.Parser.s "join" </> JoinRoom.urlParser)
        , Url.Parser.map RoomRoute (Url.Parser.s "room" </> Room.urlParser)
        ]


lift : (subModel -> Model) -> (subMsg -> Msg) -> ( subModel, Cmd subMsg ) -> ( Model, Cmd Msg )
lift toModel toMsg ( subModel, subCmd ) =
    ( toModel subModel, Cmd.map toMsg subCmd )



-- SUBSCRIPTIONS


subscriptions : Model -> Context -> Sub Msg
subscriptions model context =
    case model of
        HomeModel subModel ->
            Home.subscriptions subModel context
                |> Sub.map HomeMsg

        CreateRoomModel subModel ->
            CreateRoom.subscriptions subModel context
                |> Sub.map CreateRoomMsg

        JoinRoomModel subModel ->
            JoinRoom.subscriptions subModel context
                |> Sub.map JoinRoomMsg

        RoomModel subModel ->
            Room.subscriptions subModel context
                |> Sub.map RoomMsg

        NotFound ->
            Sub.none



-- WEBSOCKET


wsSubscriptions : Model -> Context -> WebSocketSub Msg
wsSubscriptions model context =
    case model of
        HomeModel subModel ->
            Home.wsSubscriptions subModel context
                |> WebSocketSub.map HomeMsg

        CreateRoomModel subModel ->
            CreateRoom.wsSubscriptions subModel context
                |> WebSocketSub.map CreateRoomMsg

        JoinRoomModel subModel ->
            JoinRoom.wsSubscriptions subModel context
                |> WebSocketSub.map JoinRoomMsg

        RoomModel subModel ->
            Room.wsSubscriptions subModel context
                |> WebSocketSub.map RoomMsg

        NotFound ->
            WebSocketSub.none



-- VIEW


view : Model -> Context -> List (Html Msg)
view model context =
    case model of
        HomeModel subModel ->
            Home.view subModel context
                |> List.map (H.map HomeMsg)

        CreateRoomModel subModel ->
            CreateRoom.view subModel context
                |> List.map (H.map CreateRoomMsg)

        JoinRoomModel subModel ->
            JoinRoom.view subModel context
                |> List.map (H.map JoinRoomMsg)

        RoomModel subModel ->
            Room.view subModel context
                |> List.map (H.map RoomMsg)

        NotFound ->
            [ H.text "This page does not exist. Think about it." ]
