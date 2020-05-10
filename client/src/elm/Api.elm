module Api exposing
    ( Context
    , Endpoint
    , context
    , createRoom
    , enterRoom
    , getRoomName
    , getRooms
    , request
    , task
    , tryEnterRoom
    )

import Http
import Json.Decode as JD
import Json.Encode as JE
import Pages.Room.Common exposing (RoomId(..))
import Task exposing (Task)


type Context
    = Context String


context : String -> Context
context apiPath =
    Context apiPath



-- ENDPOINTS


type Endpoint
    = Get String
    | Post String Http.Body


getRooms : Endpoint
getRooms =
    Get "/rooms"


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



-- REQUESTS


request :
    { endpoint : Endpoint
    , context : Context
    , msg : Result Http.Error a -> msg
    , decoder : JD.Decoder a
    }
    -> Cmd msg
request options =
    case ( options.endpoint, options.context ) of
        ( Get path, Context apiPath ) ->
            Http.request
                { method = "GET"
                , headers = getHeaders
                , url = apiPath ++ path
                , body = Http.emptyBody
                , expect = Http.expectStringResponse options.msg (decode options.decoder)
                , timeout = Nothing
                , tracker = Nothing
                }

        ( Post path body, Context apiPath ) ->
            Http.request
                { method = "POST"
                , headers = []
                , url = apiPath ++ path
                , body = body
                , expect = Http.expectStringResponse options.msg (decode options.decoder)
                , timeout = Nothing
                , tracker = Nothing
                }


task :
    { endpoint : Endpoint
    , context : Context
    , decoder : JD.Decoder a
    }
    -> Task Http.Error a
task options =
    case ( options.endpoint, options.context ) of
        ( Get path, Context apiPath ) ->
            Http.task
                { method = "GET"
                , headers = getHeaders
                , url = apiPath ++ path
                , body = Http.emptyBody
                , resolver = Http.stringResolver (decode options.decoder)
                , timeout = Nothing
                }

        ( Post path body, Context apiPath ) ->
            Http.task
                { method = "POST"
                , headers = []
                , url = apiPath ++ path
                , body = body
                , resolver = Http.stringResolver (decode options.decoder)
                , timeout = Nothing
                }


getHeaders : List Http.Header
getHeaders =
    [ Http.header "Cache-Control" "no-cache"
    , Http.header "Pragma" "no-cache"
    , Http.header "Expires" "Sat, 01 Jan 2000 00:00:00 GMT"
    ]


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
