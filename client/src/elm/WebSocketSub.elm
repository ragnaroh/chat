module WebSocketSub exposing (WebSocketSub, batch, destinations, map, msgs, none, sub)

import Json.Decode as JD


type WebSocketSub msg
    = Subscriptions (List (Subscription msg))


type alias Subscription msg =
    { destination : String
    , msg : JD.Value -> msg
    }


none : WebSocketSub msg
none =
    Subscriptions []


sub :
    { destination : String
    , msg : Result JD.Error a -> msg
    , decoder : JD.Decoder a
    }
    -> WebSocketSub msg
sub { destination, msg, decoder } =
    Subscriptions
        [ { destination = destination
          , msg = JD.decodeValue decoder >> msg
          }
        ]


map : (a -> msg) -> WebSocketSub a -> WebSocketSub msg
map f (Subscriptions subs) =
    subs
        |> List.map
            (\a ->
                { destination = a.destination
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


destinations : WebSocketSub msg -> List String
destinations (Subscriptions subs) =
    List.map .destination subs


msgs : String -> JD.Value -> WebSocketSub msg -> List msg
msgs destination value (Subscriptions subs) =
    subs
        |> List.filter (\a -> a.destination == destination)
        |> List.map (\a -> a.msg value)
