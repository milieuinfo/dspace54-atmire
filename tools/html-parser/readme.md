Install elm if you haven't already:
```
npm install -g elm
```

Downloads the necessary packages:
```
elm-package install
```

Builds and serves the webpage at http://localhost:8000
```
elm-reactor
```

To update the Vocabulary XML: copy the html from [https://www.milieuinfo.be/confluence/pages/viewpage.action?spaceKey=MERDB&title=Structuur+in+DSPACE#StructuurinDSPACE-Dossiers](the wiki) to the `input` list in Mer.elm. Just the HTML of the drop down table, not more. Then refresh the page from `elm-reactor`
