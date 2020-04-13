module Context exposing (Context)

import Browser.Navigation


type alias Context =
    { appPath : String
    , apiPath : String
    , navKey : Browser.Navigation.Key
    }
