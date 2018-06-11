module Mer exposing (..)

import Html
import HtmlParser exposing (..)
import HtmlParser.Util exposing (..)
import String.Extra exposing (..)


-- Data types


type Procedure
    = Procedure String (List ProcedureStap)


type ProcedureStap
    = ProcedureStap String (List Document)


type Document
    = Document String (List DocumentOnderdeel)


type DocumentOnderdeel
    = DocumentOnderdeel String



-- Parse HTML


parse : String -> Procedure
parse string =
    string
        |> HtmlParser.parse
        |> procedure


hasClass : Attributes -> String -> Bool
hasClass attributes value =
    List.member ( "class", value ) attributes


procedure : List Node -> Procedure
procedure list =
    let
        naam : String
        naam =
            list
                |> findElement (\tag attributes -> hasClass attributes "expand-control-text")
                |> textContent
    in
    Procedure naam (stappen list)


stappen : List Node -> List ProcedureStap
stappen list =
    let
        all : List ProcedureStap
        all =
            case getElementsByTagName "tr" list of
                headers :: firstRow :: otherRows ->
                    newProcedureStap firstRow otherRows

                _ ->
                    []

        newProcedureStap : Node -> List Node -> List ProcedureStap
        newProcedureStap =
            parseLevel ProcedureStap newDocument

        newDocument : Node -> List Node -> List Document
        newDocument =
            parseLevel Document newOnderdeel

        newOnderdeel : Node -> List Node -> List DocumentOnderdeel
        newOnderdeel =
            let
                leaf name _ =
                    DocumentOnderdeel name

                noNextLevel node list =
                    ()
            in
            parseLevel leaf noNextLevel
    in
    all


parseLevel : (String -> a -> b) -> (Node -> List Node -> a) -> Node -> List Node -> List b
parseLevel constructor nextLevel firstRow otherRows =
    case getChildNodes firstRow of
        firstCell :: otherCells ->
            let
                name =
                    singleTextContent firstCell

                span =
                    rowspan firstCell

                nextCellsProcessed =
                    nextLevel (Element "tr" [] otherCells)
                        (List.take span otherRows)

                thisLevel =
                    parseLevel constructor nextLevel

                nextRowsProcessed =
                    next span thisLevel otherRows
            in
            if validName name then
                constructor name nextCellsProcessed :: nextRowsProcessed
            else
                nextRowsProcessed

        _ ->
            []


validName : String -> Bool
validName name =
    name /= "" && name /= "/"


rowspan : Node -> Int
rowspan cell =
    getValue "rowspan" (getAttributes cell)
        |> default 1
        |> (\x -> x - 1)


next : Int -> (Node -> List Node -> List a) -> List Node -> List a
next span new otherRows =
    case List.drop span otherRows of
        [] ->
            []

        nextRow :: more ->
            new nextRow more


getChildNodes : Node -> List Node
getChildNodes node =
    case node of
        Text _ ->
            []

        Element _ _ children ->
            children

        Comment _ ->
            []


getAttributes : Node -> Attributes
getAttributes node =
    case node of
        Text _ ->
            []

        Element _ attributes _ ->
            attributes

        Comment _ ->
            []


singleTextContent : Node -> String
singleTextContent node =
    textContent [ node ]


default : Int -> Maybe String -> Int
default i maybeString =
    maybeString
        |> Maybe.map
            (String.toInt
                >> Result.toMaybe
                >> Maybe.withDefault i
            )
        |> Maybe.withDefault i



-- Data  ->  Vocabulary XML


toVocabulary : List Procedure -> String
toVocabulary procedures =
    fieldXml "mer.procedure" (List.map procedureXml procedures)
        |> removeEmptyLines


procedureXml : Procedure -> String
procedureXml (Procedure naam stappen) =
    elementXml naam (stapFieldXml stappen)


stapFieldXml : List ProcedureStap -> String
stapFieldXml stappen =
    fieldXml "mer.procedurestap" (List.map stapXml stappen)


stapXml : ProcedureStap -> String
stapXml (ProcedureStap naam documents) =
    elementXml naam (documentFieldXml documents)


documentFieldXml : List Document -> String
documentFieldXml documents =
    fieldXml "mer.document" (List.map documentXml documents)


documentXml : Document -> String
documentXml (Document naam onderdelen) =
    elementXml naam (onderdeelFieldXml onderdelen)


onderdeelFieldXml : List DocumentOnderdeel -> String
onderdeelFieldXml onderdelen =
    fieldXml "mer.documentonderdeel" (List.map onderdeelXml onderdelen)


onderdeelXml : DocumentOnderdeel -> String
onderdeelXml (DocumentOnderdeel naam) =
    elementXml naam ""


fieldXml : String -> List String -> String
fieldXml fieldName elements =
    if List.isEmpty elements then
        ""
    else
        """
<bean class="com.atmire.vocabulary.Field">
    <property name="field" value="{{fieldName}}"/>
    <property name="values">
        <util:list>
{{elements}}
        </util:list>
    </property>
</bean>
"""
            |> replace "{{fieldName}}" fieldName
            |> replace "{{elements}}" (insertSpaces 12 (String.join "\n" elements))


elementXml : String -> String -> String
elementXml value fields =
    if fields /= "" then
        """
<bean class="com.atmire.vocabulary.Value">
    <property name="value" value="{{value}}"/>
    <property name="fields">
        <util:list>
{{fields}}
        </util:list>
    </property>
</bean>
"""
            |> replace "{{value}}" value
            |> replace "{{fields}}" (insertSpaces 12 fields)
    else
        """
<bean class="com.atmire.vocabulary.Value">
    <property name="value" value="{{value}}"/>
</bean>
"""
            |> replace "{{value}}" value


insertSpaces : Int -> String -> String
insertSpaces n string =
    let
        helper n string =
            if n <= 0 then
                string
            else
                " " ++ helper (n - 1) string
    in
    string
        |> String.lines
        |> List.map (helper n)
        |> String.join "\n"


removeEmptyLines : String -> String
removeEmptyLines string =
    string
        |> String.lines
        |> List.filter (String.toList >> List.any ((/=) ' '))
        |> String.join "\n"



-- Display


main : Html.Html ()
main =
    Html.div []
        [ Html.code []
            [ Html.pre []
                [ input
                    |> List.map parse
                    |> toVocabulary
                    |> Html.text
                ]
            ]
        ]



-- Input


input : List String
input =
    [ """
<div id="expander-460456926" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-460456926" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Project-MER-2002</span></div><div id="expander-content-460456926" class="expand-content expand-hidden" style="display: none; opacity: 0;"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="confluenceTable"><colgroup> <col> <col> <col> <col> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="4" class="confluenceTd">Beslissing Kennisgeving</td><td rowspan="2" class="confluenceTd">Kennisgeving</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Volledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Onvolledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="8" class="confluenceTd"><span>Richtlijnen</span><br><br></td><td rowspan="2" class="confluenceTd">Inspraakreacties</td><td class="confluenceTd">Geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td colspan="1" class="confluenceTd">Niet-geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td rowspan="2" class="confluenceTd"><span>Advies</span><br><br></td><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td class="confluenceTd"><span>Advies</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td rowspan="2" class="confluenceTd">Richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Aanvullende richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">richtlijnen</td><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing MER</td><td rowspan="3" class="confluenceTd">MER</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Afkeuringsverslag</td><td class="confluenceTd"><br></td></tr></tbody></table></div><div id="floating-scrollbar" style="position: fixed; bottom: 0px; height: 30px; overflow-x: auto; overflow-y: hidden; display: block; left: 345px; width: 598px;"><div style="border: 1px solid rgb(255, 255, 255); opacity: 0.01; width: 608px;"></div></div><p> </p></div></div>
"""
    , """
<div id="expander-2013325804" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-2013325804" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Ontheffing Project-MER</span></div><div id="expander-content-2013325804" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="relative-table confluenceTable" style="width: 35.1307%;"><colgroup> <col style="width: 17.4622%;"> <col style="width: 17.5786%;"> <col style="width: 22.8172%;"> <col style="width: 42.142%;"> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="7" class="confluenceTd">Beslissing ontheffing</td><td rowspan="2" class="confluenceTd">Ontheffingsaanvraag</td><td colspan="1" class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd">Adviesvraag</td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td colspan="1" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing ontheffing</td><td colspan="1" class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Ontheffingsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Weigeringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    , """
<div id="expander-961841866" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-961841866" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Project-MER-2017</span></div><div id="expander-content-961841866" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="confluenceTable"><colgroup> <col> <col> <col> <col> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="4" class="confluenceTd">Beslissing Aanmelding</td><td rowspan="2" class="confluenceTd">Aanmelding</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Volledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Onvolledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Scopingadvies<br><br></td><td rowspan="2" class="confluenceTd">Advies</td><td class="confluenceTd">Adviesvraag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd">Inspraakreacties</td><td colspan="1" class="confluenceTd">Geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td colspan="1" class="confluenceTd">Niet-geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td rowspan="2" class="confluenceTd">Scopingadvies</td><td class="confluenceTd">Scopingadvies</td><td class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Begeleidende brief</td><td class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Voorlopige goedkeuring</td><td rowspan="3" class="confluenceTd">Ontwerp-MER</td><td colspan="1" class="confluenceTd">Niet-confidentieel deel</td><td colspan="1" class="confluenceTd">lijkt me een vreemde procedurestap.</td></tr><tr><td colspan="1" class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing ontwerp-MER</td><td colspan="1" class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Afkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="10" class="confluenceTd">Beslissing MER</td><td rowspan="3" class="confluenceTd">MER</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Advies</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd"><span>Inspraakreacties</span></td><td colspan="1" class="confluenceTd"><span>Geanonimiseerde inspraakreacties</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Niet-geanonimiseerde inspraakreacties</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Afkeuringsverslag</td><td class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    , """
<div id="expander-1856839564" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-1856839564" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Plan-MER-2004</span></div><div id="expander-content-1856839564" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="confluenceTable"><colgroup> <col> <col> <col> <col> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="4" class="confluenceTd">Beslissing Kennisgeving</td><td rowspan="2" class="confluenceTd">Kennisgeving</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Volledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Onvolledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="8" class="confluenceTd"><span>Richtlijnen</span><br><br></td><td rowspan="2" class="confluenceTd">Inspraakreacties</td><td class="confluenceTd">Geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td colspan="1" class="confluenceTd">Niet-geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td rowspan="2" class="confluenceTd"><span>Advies</span><br><br></td><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td rowspan="2" class="confluenceTd">Richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd">Aanvullende richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing MER</td><td rowspan="3" class="confluenceTd">MER</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Afkeuringsverslag</td><td class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    , """
<div id="expander-1906978385" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-1906978385" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Plan-MER Screening</span></div><div id="expander-content-1906978385" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="relative-table confluenceTable" style="width: 35.1307%;"><colgroup> <col style="width: 17.4622%;"> <col style="width: 17.5786%;"> <col style="width: 22.8172%;"> <col style="width: 42.142%;"> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="4" class="confluenceTd">Beslissing ontheffing</td><td class="confluenceTd">Ontheffingsaanvraag</td><td colspan="1" class="confluenceTd"><br></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd">Adviesvraag</td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td colspan="1" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td class="confluenceTd">Beslissing ontheffing</td><td colspan="1" class="confluenceTd"><br></td><td colspan="1" class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    , """
<div id="expander-785894317" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-785894317" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Plan-MER Integratiespoor 2007</span></div><div id="expander-content-785894317" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="confluenceTable"><colgroup> <col> <col> <col> <col> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="4" class="confluenceTd">Beslissing Kennisgeving</td><td rowspan="2" class="confluenceTd">Kennisgeving</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Volledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Onvolledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="8" class="confluenceTd"><span>Richtlijnen</span><br><br></td><td rowspan="2" class="confluenceTd">Inspraakreacties</td><td class="confluenceTd">Geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td colspan="1" class="confluenceTd">Niet-geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td rowspan="2" class="confluenceTd"><span>Advies</span></td><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td rowspan="2" class="confluenceTd">Richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Aanvullende richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">richtlijnen</td><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing MER</td><td rowspan="3" class="confluenceTd">MER</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Afkeuringsverslag</td><td class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    , """
<div id="expander-1839384072" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-1839384072" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Plan-MER-2017</span></div><div id="expander-content-1839384072" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="confluenceTable"><colgroup> <col> <col> <col> <col> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="4" class="confluenceTd">Beslissing Kennisgeving</td><td rowspan="2" class="confluenceTd">Kennisgeving</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Volledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Onvolledigverklaring</td><td class="confluenceTd">/</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="8" class="confluenceTd"><span>Richtlijnen</span><br><br></td><td rowspan="2" class="confluenceTd">Inspraakreacties</td><td class="confluenceTd">Geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td colspan="1" class="confluenceTd">Niet-geanonimiseerde inspraakreacties</td><td colspan="1" class="confluenceTd">In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</td></tr><tr><td rowspan="2" class="confluenceTd"><span>Advies</span><br><br></td><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td class="confluenceTd"><span>Adviesvraag</span></td><td colspan="1" class="confluenceTd">Gaan we deze afzonderlijk registreren?</td></tr><tr><td rowspan="2" class="confluenceTd">Richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd">Aanvullende richtlijnen</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Richtlijnen</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing Ontwerp-MER</td><td rowspan="3" class="confluenceTd"><span>MER</span></td><td colspan="1" class="confluenceTd"><span>Niet-confidentieel onderdeel</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Confidentieel deel</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Niet technische samenvatting</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd"><span>Beslissing</span></td><td colspan="1" class="confluenceTd"><span>Begeleidende brief</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Goedkeuringsverslag</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Afkeuringsverslag</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing MER</td><td rowspan="3" class="confluenceTd">MER</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Afkeuringsverslag</td><td class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    , """
<div id="expander-1648234310" class="expand-container conf-macro output-block" data-hasbody="true" data-macro-name="expand"><div id="expander-control-1648234310" class="expand-control"><span class="expand-control-icon icon">&nbsp;</span><span class="expand-control-text">Integratie m.e.r. in RUP 2017</span></div><div id="expander-content-1648234310" class="expand-content expand-hidden"><p> </p><div class="table-wrap conf-macro output-inline" data-hasbody="false" data-macro-name="multiexcerpt-include" style=""><table class="confluenceTable"><colgroup> <col> <col> <col> <col> </colgroup><tbody><tr><th class="confluenceTh">PROCEDURESTAP</th><th class="confluenceTh">DOCUMENT</th><th class="confluenceTh">DOCUMENTONDERDEEL</th><th colspan="1" class="confluenceTh">Opmerking</th></tr><tr><td rowspan="3" class="confluenceTd">Startnota</td><td colspan="1" class="confluenceTd">Startnota</td><td colspan="1" class="confluenceTd"><br></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="2" class="confluenceTd"><span>Inspraakreacties</span></td><td colspan="1" class="confluenceTd"><span>Geanonimiseerde inspraakreacties</span></td><td colspan="1" class="confluenceTd"><span>In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</span></td></tr><tr><td colspan="1" class="confluenceTd"><span>Niet-geanonimiseerde inspraakreacties</span></td><td colspan="1" class="confluenceTd"><span>In eerste instantie gaan we inspraakreacties niet allemaal als afzonderlijke items registreren.</span></td></tr><tr><td colspan="1" class="confluenceTd">Procesnota</td><td colspan="1" class="confluenceTd">Procesnota</td><td colspan="1" class="confluenceTd"><br></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="4" class="confluenceTd">Scopingnota</td><td rowspan="2" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd">Adviesvraag</td><td colspan="1" class="confluenceTd"><span>Gaan we deze afzonderlijk registreren?</span></td></tr><tr><td colspan="1" class="confluenceTd">Advies</td><td colspan="1" class="confluenceTd"><span>Gaan we deze afzonderlijk registreren?</span></td></tr><tr><td colspan="1" class="confluenceTd">Scopingnota</td><td colspan="1" class="confluenceTd"><br></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd">Bepaling mbt. planMER-plicht</td><td colspan="1" class="confluenceTd"><br></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing Ontwerp-MER</td><td rowspan="3" class="confluenceTd"><span>MER</span></td><td colspan="1" class="confluenceTd"><span>Niet-confidentieel onderdeel</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Confidentieel deel</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Niet technische samenvatting</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd"><span>Beslissing</span></td><td colspan="1" class="confluenceTd"><span>Begeleidende brief</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Goedkeuringsverslag</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td colspan="1" class="confluenceTd"><span>Afkeuringsverslag</span></td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="6" class="confluenceTd">Beslissing MER</td><td rowspan="3" class="confluenceTd">MER</td><td class="confluenceTd">Niet-confidentieel onderdeel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Confidentieel deel</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Niet technische samenvatting</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td rowspan="3" class="confluenceTd">Beslissing</td><td class="confluenceTd">Begeleidende brief</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Goedkeuringsverslag</td><td colspan="1" class="confluenceTd"><br></td></tr><tr><td class="confluenceTd">Afkeuringsverslag</td><td class="confluenceTd"><br></td></tr></tbody></table></div><p> </p></div></div>
"""
    ]
