module Pages.Room exposing
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
import Http
import Json.Decode as JD
import Pages.Room.Common exposing (RoomId(..), RoomName(..), Username(..))
import Pages.Room.Inside as Inside
import Pages.Room.Lobby as Lobby
import Url.Parser
import WebSocketSub exposing (WebSocketSub)


type alias Model =
    { roomId : RoomId
    , subModel : SubModel
    }


type SubModel
    = Initializing (Maybe RoomName) (Maybe Bool)
    | LobbyModel RoomName Lobby.Model
    | InsideModel RoomName Inside.Model


type Msg
    = LobbyMsg Lobby.Msg
    | InsideMsg Inside.Msg
    | ReceiveRoomName (Result Http.Error RoomName)
    | ReceiveEnterRoomResponse (Result Http.Error Bool)


type alias Route =
    String



-- INIT


init : Route -> Context -> ( Model, Cmd Msg )
init route context =
    let
        roomId =
            RoomId route
    in
    ( { roomId = roomId
      , subModel = Initializing Nothing Nothing
      }
    , Cmd.batch
        [ fetchRoomNameCmd roomId context
        , tryEnterRoomCmd roomId context
        ]
    )


fetchRoomNameCmd : RoomId -> Context -> Cmd Msg
fetchRoomNameCmd roomId context =
    Api.request
        { endpoint = Api.getRoomName roomId
        , context = context
        , msg = ReceiveRoomName
        , decoder = JD.string |> JD.map RoomName
        }


tryEnterRoomCmd : RoomId -> Context -> Cmd Msg
tryEnterRoomCmd roomId context =
    Api.request
        { endpoint = Api.tryEnterRoom roomId
        , context = context
        , msg = ReceiveEnterRoomResponse
        , decoder = JD.bool
        }



-- ROUTES


urlParser : Url.Parser.Parser (Route -> a) a
urlParser =
    Url.Parser.string



-- UPDATE


update : Msg -> Model -> Context -> ( Model, Cmd Msg )
update msg model context =
    updateSub msg model.subModel model.roomId context
        |> Tuple.mapFirst (\newSubModel -> { model | subModel = newSubModel })


updateSub : Msg -> SubModel -> RoomId -> Context -> ( SubModel, Cmd Msg )
updateSub msg model roomId context =
    case ( msg, model ) of
        ( ReceiveRoomName (Ok roomName), Initializing Nothing Nothing ) ->
            ( Initializing (Just roomName) Nothing, Cmd.none )

        ( ReceiveRoomName (Ok roomName), Initializing Nothing (Just False) ) ->
            Lobby.init
                |> Tuple.mapFirst (LobbyModel roomName)
                |> Tuple.mapSecond (Cmd.map LobbyMsg)

        ( ReceiveRoomName (Ok roomName), Initializing Nothing (Just True) ) ->
            Inside.init
                |> Tuple.mapFirst (InsideModel roomName)
                |> Tuple.mapSecond (Cmd.map InsideMsg)

        ( ReceiveEnterRoomResponse (Ok userExists), Initializing Nothing Nothing ) ->
            ( Initializing Nothing (Just userExists), Cmd.none )

        ( ReceiveEnterRoomResponse (Ok False), Initializing (Just roomName) Nothing ) ->
            Lobby.init
                |> Tuple.mapFirst (LobbyModel roomName)
                |> Tuple.mapSecond (Cmd.map LobbyMsg)

        ( ReceiveEnterRoomResponse (Ok True), Initializing (Just roomName) Nothing ) ->
            Inside.init
                |> Tuple.mapFirst (InsideModel roomName)
                |> Tuple.mapSecond (Cmd.map InsideMsg)

        ( LobbyMsg subMsg, LobbyModel roomName subModel ) ->
            case Lobby.update subMsg subModel roomId context of
                Lobby.ModelAndCmd newModel cmd ->
                    ( LobbyModel roomName newModel
                    , Cmd.map LobbyMsg cmd
                    )

                Lobby.EnterRoom ->
                    Inside.init
                        |> Tuple.mapFirst (InsideModel roomName)
                        |> Tuple.mapSecond (Cmd.map InsideMsg)

                Lobby.None ->
                    ( model, Cmd.none )

        ( InsideMsg subMsg, InsideModel roomName subModel ) ->
            Inside.update subMsg subModel roomId context
                |> Tuple.mapFirst (InsideModel roomName)
                |> Tuple.mapSecond (Cmd.map InsideMsg)

        _ ->
            ( model, Cmd.none )



-- SUBSCRIPTIONS


subscriptions : Model -> Context -> Sub Msg
subscriptions _ _ =
    Sub.none



-- WEBSOCKET


wsSubscriptions : Model -> Context -> WebSocketSub Msg
wsSubscriptions model _ =
    case model.subModel of
        Initializing _ _ ->
            WebSocketSub.none

        LobbyModel _ _ ->
            WebSocketSub.none

        InsideModel _ subModel ->
            Inside.wsSubscriptions subModel model.roomId
                |> WebSocketSub.map InsideMsg



-- VIEW


view : Model -> Context -> List (Html Msg)
view model context =
    case model.subModel of
        Initializing _ _ ->
            []

        LobbyModel roomName subModel ->
            Lobby.view subModel roomName context
                |> List.map (H.map LobbyMsg)

        InsideModel roomName subModel ->
            Inside.view subModel roomName context
                |> List.map (H.map InsideMsg)
