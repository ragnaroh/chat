module Form exposing (onEnter)

import Html as H
import Html.Events as HE
import Json.Decode as JD


onEnter : msg -> H.Attribute msg
onEnter msg =
    HE.keyCode
        |> JD.andThen
            (\key ->
                if key == 13 then
                    JD.succeed msg

                else
                    JD.fail "Not enter"
            )
        |> HE.on "keydown"
