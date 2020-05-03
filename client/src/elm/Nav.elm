module Nav exposing
    ( Context
    , Path
    , baseUrlParser
    , context
    , create
    , href
    , join
    , loadPath
    , pushPath
    , pushUrl
    , replacePath
    , room
    , top
    )

import Browser.Navigation
import Html as H
import Html.Attributes as HA
import Pages.Room.Common exposing (RoomId(..))
import Url exposing (Url)
import Url.Parser exposing ((</>))


type Context
    = Context String Browser.Navigation.Key


context : String -> Browser.Navigation.Key -> Context
context appPath navKey =
    Context appPath navKey


baseUrlParser : Context -> Url.Parser.Parser (route -> a) (route -> a)
baseUrlParser (Context appPath _) =
    String.split "/" appPath
        |> List.filter (not << String.isEmpty)
        |> List.foldl (\s acc -> acc </> Url.Parser.s s) Url.Parser.top



-- PATHS


type Path
    = Path String


top : Path
top =
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
href (Context appPath _) (Path path) =
    HA.href (appPath ++ path)


pushPath : Context -> Path -> Cmd msg
pushPath (Context appPath navKey) (Path path) =
    Browser.Navigation.pushUrl navKey (appPath ++ path)


replacePath : Context -> Path -> Cmd msg
replacePath (Context appPath navKey) (Path path) =
    Browser.Navigation.replaceUrl navKey (appPath ++ path)


pushUrl : Context -> Url -> Cmd msg
pushUrl (Context _ navKey) url =
    Browser.Navigation.pushUrl navKey (Url.toString url)


loadPath : Context -> Path -> Cmd msg
loadPath (Context appPath _) (Path path) =
    Browser.Navigation.load (appPath ++ path)
