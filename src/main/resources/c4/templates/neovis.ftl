(function draw() {
    var config = {
        container_id: "viz",
        server_url: "bolt://localhost:7687",
        labels: {
            "Person": {
                "caption": "value",
                "size": 2.0
            },
            "Technology": {
                "caption": "value",
                "size": 1.5
            },
            "Team": {
                "caption": "value",
                "size": 1.5
            },
            "Company": {
                "caption": "value",
                "size": 2.0
            },
            "Text": {
                "caption": "value",
                "size": 1.0
            },
            "Blank": {
                "caption": "value",
                "size": 1.5
            },
            "Other": {
                "caption": "value",
                "size": 1.0
            },
            "Date": {
                "caption": "value",
                "size": 1.5
            }
        },
        relationships: {
            "same as": {
                "thickness": "1"
            },
            "more like": {
                "thickness": "1"
            }
        },
        initial_cypher: 'MATCH (p:Person {user: "${js_person}"})-[r*]->(m) WHERE m.value <> "more like" RETURN *'
    }

    var viz = new NeoVis.default(config)
    viz.render()
})();
