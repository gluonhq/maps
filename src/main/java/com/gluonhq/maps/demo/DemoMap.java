/*
 * Copyright (c) 2016, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.maps.demo;

import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.Position;
import com.gluonhq.charm.down.plugins.PositionService;
import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 *
 * Demo class showing a simple map app
 */
public class DemoMap extends Application {

    private static final Logger LOGGER = Logger.getLogger(DemoMap.class.getName());

    static {
        try {
            LogManager.getLogManager().readConfiguration(DemoMap.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading logging properties file", e);
        }
    }
    
    private MapPoint mapPoint;
            
    @Override
    public void start(Stage stage) throws Exception {

        MapView view = new MapView();
        view.addLayer(positionLayer());
        view.setZoom(3); 
        Scene scene;
        if (Platform.isDesktop()) {
            scene = new Scene(view, 600, 700);
            stage.setTitle("Gluon Maps Demo");
        } else {
            BorderPane bp = new BorderPane();
            bp.setCenter(view);
            final Label label = new Label("Gluon Maps Demo");
            label.setAlignment(Pos.CENTER);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-background-color: dimgrey; -fx-text-fill: white;");
            bp.setTop(label);
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            scene = new Scene(bp, bounds.getWidth(), bounds.getHeight());
        }
        stage.setScene(scene);
        stage.show();

        view.flyTo(1., mapPoint, 2.);
    }
    
//    private MapLayer myDemoLayer () {
//        PoiLayer answer = new PoiLayer();
//        Node icon1 = new Circle(7, Color.BLUE);
//        answer.addPoint(new MapPoint(50.8458,4.724), icon1);
//        Node icon2 = new Circle(7, Color.GREEN);
//        answer.addPoint(new MapPoint(37.396256,-121.953847), icon2);
//        return answer;
//    }
    
    private MapLayer positionLayer() {
        return Services.get(PositionService.class)
                .map(positionService -> {
                    ReadOnlyObjectProperty<Position> positionProperty = positionService.positionProperty();
                    Position position = positionProperty.get();
                    if (position == null) {
                        position = new Position(50.,4.);
                    }
                    mapPoint = new MapPoint(position.getLatitude(), position.getLongitude());
                    LOGGER.log(Level.INFO, "Initial Position: " + position.getLatitude() + ", " + position.getLongitude());
                        
                    PoiLayer answer = new PoiLayer();
                    answer.addPoint(mapPoint, new Circle(7, Color.RED));

                    positionProperty.addListener(e -> {
                        Position pos = positionProperty.get();
                        LOGGER.log(Level.INFO, "New Position: " + pos.getLatitude() + ", " + pos.getLongitude());
                        mapPoint.update(pos.getLatitude(), pos.getLongitude());
                    });
                    return answer;
                })
                .orElseGet(() -> {
                    LOGGER.log(Level.WARNING, "Position Service not available");
                    PoiLayer answer = new PoiLayer();
                    mapPoint = new MapPoint(50., 4.);
                    answer.addPoint(mapPoint, new Circle(7, Color.RED));
                    return answer;
                });
    }
}
