module WebSocketSub exposing
    ( Endpoint
    , WebSocketSub
    , batch
    , endpoints
    , map
    , msgs
    , none
    , roomTopic
    , roomUserQueue
    , sub
    )

import Json.Decode as JD
import Pages.Room.Common exposing (RoomId(..))



-- ENDPOINTS


type Endpoint
    = Endpoint String


roomTopic : RoomId -> Endpoint
roomTopic (RoomId roomId) =
    Endpoint ("/topic/room/" ++ roomId)


roomUserQueue : RoomId -> Endpoint
roomUserQueue (RoomId roomId) =
    Endpoint ("/user/queue/room/" ++ roomId)



-- SUBSCRIPTIONS


type WebSocketSub msg
    = Subscriptions (List (Subscription msg))


type alias Subscription msg =
    { endpoint : Endpoint
    , msg : JD.Value -> msg
    }


none : WebSocketSub msg
none =
    Subscriptions []


sub :
    { endpoint : Endpoint
    , msg : Result JD.Error a -> msg
    , decoder : JD.Decoder a
    }
    -> WebSocketSub msg
sub { endpoint, msg, decoder } =
    Subscriptions
        [ { endpoint = endpoint
          , msg = JD.decodeValue decoder >> msg
          }
        ]


map : (a -> msg) -> WebSocketSub a -> WebSocketSub msg
map f (Subscriptions subs) =
    subs
        |> List.map
            (\a ->
                { endpoint = a.endpoint
                , msg = a.msg >> f
                }
            )
        |> Subscriptions


batch : List (WebSocketSub msg) -> WebSocketSub msg
batch list =
    list
        |> List.map unwrapSub
        |> List.concat
        |> Subscriptions


endpoints : WebSocketSub msg -> List String
endpoints (Subscriptions subs) =
    List.map .endpoint subs
        |> List.map unwrapEndpoint


msgs : String -> JD.Value -> WebSocketSub msg -> List msg
msgs endpoint value (Subscriptions subs) =
    subs
        |> List.filter (.endpoint >> unwrapEndpoint >> (==) endpoint)
        |> List.map (\{ msg } -> msg value)


unwrapSub : WebSocketSub msg -> List (Subscription msg)
unwrapSub (Subscriptions subs) =
    subs


unwrapEndpoint : Endpoint -> String
unwrapEndpoint (Endpoint endpoint) =
    endpoint
