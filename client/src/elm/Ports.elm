port module Ports exposing
    ( connectionStatusIn
    , leaveRoom
    , refreshWebSocketSubscriptions
    , roomMessageOut
    , webSocketMessageIn
    )

import Json.Decode as JD
import Json.Encode as JE


port connectionStatusIn : (JD.Value -> msg) -> Sub msg


port webSocketMessageIn : (JD.Value -> msg) -> Sub msg


port roomMessageOut : JE.Value -> Cmd msg


port leaveRoom : JE.Value -> Cmd msg


port refreshWebSocketSubscriptions : List String -> Cmd msg
