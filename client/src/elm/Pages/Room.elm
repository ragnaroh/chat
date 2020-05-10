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
import Task exposing (Task)
import Url.Parser
import WebSocketSub exposing (WebSocketSub)


type alias Model =
    { roomId : RoomId
    , subModel : SubModel
    }


type SubModel
    = Initializing
    | LobbyModel RoomName Lobby.Model
    | InsideModel RoomName Inside.Model


type Msg
    = GotInitialData (Result Http.Error InitialData)
    | LobbyMsg Lobby.Msg
    | InsideMsg Inside.Msg


type alias Route =
    String


type alias InitialData =
    { roomName : RoomName
    , entered : Bool
    }



-- INIT


init : Route -> Context -> ( Model, Cmd Msg )
init route context =
    let
        roomId =
            RoomId route
    in
    ( { roomId = roomId
      , subModel = Initializing
      }
    , Task.map2 InitialData
        (fetchRoomNameTask roomId context)
        (tryEnterRoomTask roomId context)
        |> Task.attempt GotInitialData
    )


fetchRoomNameTask : RoomId -> Context -> Task Http.Error RoomName
fetchRoomNameTask roomId context =
    Api.task
        { endpoint = Api.getRoomName roomId
        , context = context.api
        , decoder = JD.string |> JD.map RoomName
        }


tryEnterRoomTask : RoomId -> Context -> Task Http.Error Bool
tryEnterRoomTask roomId context =
    Api.task
        { endpoint = Api.tryEnterRoom roomId
        , context = context.api
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
        ( GotInitialData (Ok { roomName, entered }), Initializing ) ->
            if entered then
                Inside.init
                    |> Tuple.mapFirst (InsideModel roomName)
                    |> Tuple.mapSecond (Cmd.map InsideMsg)

            else
                Lobby.init
                    |> Tuple.mapFirst (LobbyModel roomName)
                    |> Tuple.mapSecond (Cmd.map LobbyMsg)

        ( GotInitialData (Err _), Initializing ) ->
            ( model, Cmd.none )

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
        Initializing ->
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
        Initializing ->
            []

        LobbyModel roomName subModel ->
            Lobby.view subModel roomName context
                |> List.map (H.map LobbyMsg)

        InsideModel roomName subModel ->
            Inside.view subModel roomName context
                |> List.map (H.map InsideMsg)
