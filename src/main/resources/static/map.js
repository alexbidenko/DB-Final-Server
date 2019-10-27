this.map.addObject(this.mapGroup);

this.map.addEventListener('mapviewchange', function() {
    clearTimeout(window.update_timeout);
    window.update_timeout = setTimeout(updateMap(this.map), 2000);
});

const drawBuffer = (data) => {
    if(window.isBufferDraw) {
        const points = [];
        data.forEach(object => {
            let coordinates = JSON.parse(object.coordinates);
            coordinates.forEach(c => {
                points.push([c.lng, c.lat]);
            });
        });
        let point = turf.point(points);
        let buffered = turf.buffer(point, 0.1, {units: 'miles'});

        const line = new H.geo.LineString();

        buffered.geometry.coordinates.forEach(coordinate => {
            line.pushLatLngAlt(coordinate[1], coordinate[0]);
        });

        this.mapGroup.addObject(
            new H.map.Polygon(line, {
                style: {
                    fillColor: '#44FF4440',
                    strokeColor: '#444',
                    lineWidth: 3
                }
            })
        );
    }
};

const updateMap = map => {
    if(!window.lastUpdate || (new H.map.Marker(window.lastUpdate, {})).getPosition()
        .distance((new H.map.Marker(map.getCenter(), {})).getPosition()) > 2000) {
        window.lastUpdate = map.getCenter();

        fetch(`http://52.14.34.221:2019/api/v1.0/map/list?latitude=${map.getCenter().lat}&longitude=${map.getCenter().lng}&radius=1&type=1`).then(response => {
            return response.json();
        }).then(data => {
            window.mapData = data;
            this.mapGroup.removeAllObjects();
            data.forEach(object => {
                this.drawLine(this.mapGroup, JSON.parse(object.coordinates), false);
            });
            drawBuffer(window.mapData);
        });
    }
};

const drawMarker = (data) => {
    const svgMarkup = `<svg width="18" height="18"
        xmlns="http://www.w3.org/2000/svg">
        <circle cx="8" cy="8" r="8"
            fill="#ddd" stroke="#4527a0" stroke-width="1"  />
        </svg>`,
        dotIcon = new H.map.Icon(svgMarkup, {anchor: {x:8, y:8}});

    const marker =  new H.map.Marker({
            lat: data.latitude,
            lng: data.longitude},
        {icon: dotIcon});
    marker.setData({data: data});
    this.mapGroup.addObject(marker);
};

this.map.addEventListener('tap', function (evt) {
    const coordinates = this.map.screenToGeo(evt.currentPointer.viewportX,
        evt.currentPointer.viewportY);
});

const addPolygon = data => {
    const line = new H.geo.LineString();

    data.forEach(coordinate => {
        line.pushLatLngAlt(coordinate.lat, coordinate.lng);
    });

    this.mapGroup.addObject(
        new H.map.Polygon(line, {
            style: {
                fillColor: '#FFFFCC',
                strokeColor: '#829',
                lineWidth: 8
            }
        })
    );
};

const drawLine = (group, data, isError) => {
    const line = new H.geo.LineString();

    data.forEach(coordinate => {
        line.pushLatLngAlt(coordinate.lat, coordinate.lng);
    });

    const polyline = new H.map.Polyline(line, {
        style: {
            lineWidth  : 4,
            strokeColor: isError ? '#ff27a099' : '#4527a099'
        }
    });

    group.addObject(polyline);
};

const initCrosser = map => {
    const objectsGroup = new H.map.Group();
    map.addObject(objectsGroup);
    const errorGroup = new H.map.Group();
    map.addObject(errorGroup);

    window.cashBeforeClick = null;
    window.lineForSend = [];

    let isEnable = true;

    map.addEventListener('tap', evt => {
        const coordinates = this.map.screenToGeo(evt.currentPointer.viewportX,
            evt.currentPointer.viewportY);

        if(!isEnable) {
        } else if(window.cashBeforeClick == null) {
            window.cashBeforeClick = coordinates;
            window.lineForSend.push(coordinates);
        } else {
            isEnable = false;
            errorGroup.removeAllObjects();

            fetch("http://52.14.34.221:2019/api/v1.0/map/check", {
                method: 'POST',
                body: JSON.stringify({
                    "type": 1,
                    "latitude": coordinates.lat,
                    "longitude": coordinates.lng,
                    "category": "",
                    "coordinates":
                        JSON.stringify([
                            {lat:window.cashBeforeClick.lat,lng:window.cashBeforeClick.lng},
                            {lat:coordinates.lat,lng:coordinates.lng}
                            ])
                }),
                headers: {
                    'Content-Type': 'application/json;charset=utf-8'
                }
            }).then(response => {
                isEnable = true;
                if(response.code() === 200) {
                    objectsGroup.removeAllObjects();
                    window.lineForSend.push(coordinates);
                    this.drawLine(objectsGroup, window.lineForSend, false)
                } else {
                    this.drawLine(errorGroup, [
                        {lat:window.cashBeforeClick.lat,lng:window.cashBeforeClick.lng},
                        {lat:coordinates.lat,lng:coordinates.lng}
                    ], true)
                }
            });
        }
    });
};