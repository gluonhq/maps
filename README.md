[![Gluon](.github/assets/gluon_logo.svg)](https://gluonhq.com)

[![Build](https://github.com/gluonhq/maps/actions/workflows/build.yml/badge.svg)](https://github.com/gluonhq/maps/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.gluonhq/maps)](https://search.maven.org/#search|ga|1|com.gluonhq.maps)
[![License](https://img.shields.io/github/license/gluonhq/maps)](https://opensource.org/licenses/GPL-3.0)
[![javadoc](https://javadoc.io/badge2/com.gluonhq/maps/javadoc.svg)](https://javadoc.io/doc/com.gluonhq/maps)

# Gluon Maps

Gluon Maps provides an easy way to integrate [OpenStreetMaps](https://www.openstreetmap.org) into a JavaFX application.
It is blazing fast and offers layer overlays, multiple tile-sets, and much more.

## API Overview

### MapPoint
Represents a set of latitude and longitude values.

```
MapPoint point = new MapPoint(latitude, longitude);
```

### MapLayer
Shows the map tiles along with any user defined nodes.
This class can be extended by overloading the `layoutLayer` method.

`The following code snippet shows how to create a custom layer that displays red circles at the given map points:

```
public class CustomMapLayer extends MapLayer {

    @Override
    protected void layoutLayer() {
        for (Pair<MapPoint, Node> candidate : points) {
            MapPoint point = new MapPoint(lat, long);
            Node icon = new Circle(5, Color.RED);
            Point2D mapPoint = getMapPoint(point.getLatitude(), point.getLongitude());
            icon.setVisible(true);
            icon.setTranslateX(mapPoint.getX());
            icon.setTranslateY(mapPoint.getY());
        }
    }
}
```

### MapView
Top level map component which supports multiple `MapLayer`.
The section of viewable map can be changed using input events (mouse/touch/gestures),
or by calling the methods `setCenter` and `setZoom` directly.

```
MapView mapView = new MapView();
mapView.addLayer(new CustomMapLayer());
mapView.setZoom(3);
```

## Usage

`Map` can be added to a JavaFX project by using the artifact published in Maven Central:

Maven:
```
<dependency>
    <groupId>com.gluonhq</groupId>
    <artifactId>emoji</artifactId>
    <version>${version}</version>
</dependency>
```

Gradle:
```
dependencies {
    implementation 'com.gluonhq:emoji:${version}'
}
```

The project can be also be installed in the local Maven repository:

```
mvn install
```

## Sample

A [sample](https://github.com/gluonhq/maps/tree/master/samples/) is provided in the repository to get started with the library.

To run the sample:

```
mvn javafx:run -f samples
```

## Contribution

All contributions are welcome!

There are two common ways to contribute:

- Submit [issues](https://github.com/gluonhq/maps/issues) for bug reports, questions, or requests for enhancements.
- Contributions can be submitted via [pull request](https://github.com/gluonhq/maps/pulls), provided you have signed the [Gluon Individual Contributor License Agreement (CLA)](https://cla.gluonhq.com).

Follow [contributing rules](https://github.com/gluonhq/maps/blob/master/CONTRIBUTING.md) for this repository.

## Commercial License

Commercial licences available at: http://gluonhq.com/labs/maps/buy/
