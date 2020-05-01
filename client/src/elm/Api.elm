module Api exposing (Endpoint, createRoom, enterRoom, getRoomName, request, tryEnterRoom)

import Context exposing (Context)
import Http
import Json.Decode as JD
import Json.Encode as JE
import Pages.Room.Common exposing (RoomId(..))


type Endpoint
    = Get String
    | Post String Http.Body


getRoomName : RoomId -> Endpoint
getRoomName (RoomId roomId) =
    Get ("/rooms/" ++ roomId ++ "/name")


createRoom : String -> Endpoint
createRoom name =
    Post "/rooms/create" (Http.jsonBody (JE.string name))


tryEnterRoom : RoomId -> Endpoint
tryEnterRoom (RoomId roomId) =
    Post ("/rooms/" ++ roomId ++ "/try-enter") Http.emptyBody


enterRoom : RoomId -> String -> Endpoint
enterRoom (RoomId roomId) username =
    Post ("/rooms/" ++ roomId ++ "/enter") (Http.jsonBody (JE.string username))


request :
    { endpoint : Endpoint
    , context : Context
    , msg : Result Http.Error a -> msg
    , decoder : JD.Decoder a
    }
    -> Cmd msg
request { endpoint, context, msg, decoder } =
    case endpoint of
        Get path ->
            Http.request
                { method = "GET"
                , headers =
                    [ Http.header "Cache-Control" "no-cache"
                    , Http.header "Pragma" "no-cache"
                    , Http.header "Expires" "Sat, 01 Jan 2000 00:00:00 GMT"
                    ]
                , url = context.apiPath ++ path
                , body = Http.emptyBody
                , expect = Http.expectStringResponse msg (decode decoder)
                , timeout = Nothing
                , tracker = Nothing
                }

        Post path body ->
            Http.request
                { method = "POST"
                , headers = []
                , url = context.apiPath ++ path
                , body = body
                , expect = Http.expectStringResponse msg (decode decoder)
                , timeout = Nothing
                , tracker = Nothing
                }


decode : JD.Decoder a -> Http.Response String -> Result Http.Error a
decode decoder response =
    case response of
        Http.BadUrl_ url ->
            Err (Http.BadUrl url)

        Http.Timeout_ ->
            Err Http.Timeout

        Http.NetworkError_ ->
            Err Http.NetworkError

        Http.BadStatus_ metadata _ ->
            Err (Http.BadStatus metadata.statusCode)

        Http.GoodStatus_ _ body ->
            case JD.decodeString decoder body of
                Ok value ->
                    Ok value

                Err err ->
                    Err (Http.BadBody (JD.errorToString err))
