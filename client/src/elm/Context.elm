module Context exposing (Context)

import Api
import Nav


type alias Context =
    { nav : Nav.Context
    , api : Api.Context
    }
