module Navigation exposing (Path, create, home, href, join, pushUrl, room)

import Browser.Navigation
import Context exposing (Context)
import Html as H
import Html.Attributes as HA
import Pages.Room.Common exposing (RoomId(..))



-- PATHS


type Path
    = Path String


home : Path
home =
    Path "/"


create : Path
create =
    Path "/create"


join : Path
join =
    Path "/join"


room : RoomId -> Path
room (RoomId roomId) =
    Path ("/room/" ++ roomId)



-- NAVIGATION


href : Context -> Path -> H.Attribute msg
href context (Path path) =
    HA.href (context.appPath ++ path)


pushUrl : Context -> Path -> Cmd msg
pushUrl context (Path path) =
    Browser.Navigation.pushUrl context.navKey (context.appPath ++ path)
