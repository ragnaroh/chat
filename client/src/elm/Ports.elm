port module Ports exposing
    ( refreshWebSocketSubscriptions
    , roomMessageOut
    , webSocketMessageIn
    )

import Json.Decode as JD
import Json.Encode as JE


port roomMessageOut : JE.Value -> Cmd msg


port webSocketMessageIn : (JD.Value -> msg) -> Sub msg


port refreshWebSocketSubscriptions : List String -> Cmd msg
