module WebSocketSub exposing (WebSocketSub, batch, map, msgs, none, sub, topics)

import Json.Decode as JD


type WebSocketSub msg
    = Subscriptions (List (Subscription msg))


type alias Subscription msg =
    { topic : String
    , msg : JD.Value -> msg
    }


none : WebSocketSub msg
none =
    Subscriptions []


sub :
    { topic : String
    , msg : Result JD.Error a -> msg
    , decoder : JD.Decoder a
    }
    -> WebSocketSub msg
sub { topic, msg, decoder } =
    Subscriptions
        [ { topic = topic
          , msg = JD.decodeValue decoder >> msg
          }
        ]


map : (a -> msg) -> WebSocketSub a -> WebSocketSub msg
map f (Subscriptions subs) =
    subs
        |> List.map
            (\a ->
                { topic = a.topic
                , msg = a.msg >> f
                }
            )
        |> Subscriptions


batch : List (WebSocketSub msg) -> WebSocketSub msg
batch list =
    let
        extract (Subscriptions subs) =
            subs
    in
    list
        |> List.map extract
        |> List.concat
        |> Subscriptions


topics : WebSocketSub msg -> List String
topics (Subscriptions subs) =
    List.map .topic subs


msgs : String -> JD.Value -> WebSocketSub msg -> List msg
msgs topic value (Subscriptions subs) =
    subs
        |> List.filter (\a -> a.topic == topic)
        |> List.map (\a -> a.msg value)
