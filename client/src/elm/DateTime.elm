module DateTime exposing (DateTime, decoder, hhmm, hhmmss)

import Date exposing (Date)
import Json.Decode as JD


type alias DateTime =
    { date : Date
    , time : Time
    }


type alias Time =
    { hour : Int
    , minute : Int
    , second : Int
    }


decoder : JD.Decoder DateTime
decoder =
    JD.list JD.int
        |> JD.andThen
            (toDateTime
                >> Maybe.map JD.succeed
                >> Maybe.withDefault (JD.fail "Invalid datetime received")
            )


toDateTime : List Int -> Maybe DateTime
toDateTime list =
    case list of
        year :: month :: day :: hour :: minute :: second :: _ ->
            toDate year month day
                |> Maybe.map (\date -> DateTime date (Time hour minute second))

        year :: month :: day :: hour :: minute :: [] ->
            toDate year month day
                |> Maybe.map (\date -> DateTime date (Time hour minute 0))

        _ ->
            Nothing


toDate : Int -> Int -> Int -> Maybe Date
toDate year month day =
    let
        date =
            Date.fromCalendarDate year (Date.numberToMonth month) day
    in
    if
        (Date.day date == day)
            && (Date.monthToNumber (Date.month date) == month)
            && (Date.year date == year)
    then
        Just date

    else
        Nothing


hhmmss : DateTime -> String
hhmmss dateTime =
    String.join ":"
        [ dateTime.time.hour |> String.fromInt |> String.padLeft 2 '0'
        , dateTime.time.minute |> String.fromInt |> String.padLeft 2 '0'
        , dateTime.time.second |> String.fromInt |> String.padLeft 2 '0'
        ]


hhmm : DateTime -> String
hhmm dateTime =
    String.join ":"
        [ dateTime.time.hour |> String.fromInt |> String.padLeft 2 '0'
        , dateTime.time.minute |> String.fromInt |> String.padLeft 2 '0'
        ]
