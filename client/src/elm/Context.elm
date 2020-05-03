module Context exposing (Context)

import Api
import Browser.Navigation
import Navigation


type alias Context =
    { nav : Navigation.Context
    , api : Api.Context
    }
