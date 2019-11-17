package API;

import Controllers.WestminsterRentalVehicleManager;
import Models.Bike;
import Models.Car;
import Models.Vehicle;
import Models.VehicleModel;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseController {
    private MongoCollection<Document> vehicleCollection;

    public DatabaseController(MongoDatabase database) {
        this.vehicleCollection =  database.getCollection("vehicle");
    }

    public ArrayList<Vehicle> getVehicles(){
        ArrayList<Vehicle> items = new ArrayList<>();
        vehicleCollection.find().forEach((Block<Document>) document -> {
            Document vehicleModel = (Document) document.get("vehicleModel");
            if (document.getString("type").equalsIgnoreCase("car")) {
                items.add(new Car(
                        document.getString("plateNumber"),
                        ((Decimal128) document.get("costPerDay")).bigDecimalValue(),
                        new VehicleModel(
                                vehicleModel.getString("type"),
                                vehicleModel.getString("make"),
                                vehicleModel.getString("model")
                        ),
                        document.getDouble("mileage"),
                        document.getDouble("engineCapacity"),
                        document.getInteger("seats"),
                        document.getString("transmission"),
                        document.getInteger("doors"),
                        document.getBoolean("airConditioned"),
                        document.getInteger("trunkCapacity")
                ));
            } else if (document.getString("type").equalsIgnoreCase("bike")) {
                items.add(new Bike(
                        document.getString("plateNumber"),
                        ((Decimal128) document.get("costPerDay")).bigDecimalValue(),
                        new VehicleModel(
                                vehicleModel.getString("type"),
                                vehicleModel.getString("make"),
                                vehicleModel.getString("model")
                        ),
                        document.getDouble("mileage"),
                        document.getDouble("engineCapacity"),
                        document.getInteger("seats"),
                        document.getString("transmission"),
                        document.getDouble("wheelSize"),
                        document.getBoolean("sideCar"),
                        document.getInteger("numOfHelmets")
                ));
            } else{
                System.err.println("Unrecognized Vehicle Type");
                System.err.println(document.toJson());
            }
        });
        return items;
    }

    public void addVehicle(Document vehicle) throws OutOfMemoryError {
        if(this.numOfFreeParkingSlots() >= WestminsterRentalVehicleManager.MAX_VEHICLES){
            throw new OutOfMemoryError();
        }
        vehicleCollection.insertOne(vehicle);
    }

    public void deleteVehicle(String plateNumber) {
        if(isVehicle(plateNumber)){
            vehicleCollection.findOneAndDelete(eq("plateNumber", plateNumber));
            return;
        }
        throw new InvalidParameterException("PlateNumber is not in the database");
    }

    public boolean isVehicle(String plateNumber) {
        return vehicleCollection.count(eq("plateNumber", plateNumber)) > 0;
    }

    public int numOfFreeParkingSlots(){
        return (int) vehicleCollection.count();
    }

    public static Document createVehicleDocument(Map data) throws IllegalArgumentException, NullPointerException, ClassCastException {
        Map vehicleModel = (Map) data.get("vehicleModel");
        Document doc = new Document("plateNumber", data.get("plateNumber"))
                //.append("costPerDay", new BigDecimal((double) data.get("costPerDay"))) This throws a java.lang.NumberFormatException
                .append("costPerDay", new BigDecimal(String.valueOf(data.get("costPerDay"))))
                .append("vehicleModel", new Document("type", vehicleModel.get("type"))
                        .append("make", vehicleModel.get("make"))
                        .append("model", vehicleModel.get("model"))
                )
                .append("mileage", data.get("mileage"))
                .append("engineCapacity", data.get("engineCapacity"))
                .append("seats", (int) Math.round((double) data.get("seats")))
                .append("transmission", data.get("transmission"));

        if (((String) data.get("type")).equalsIgnoreCase("car")) {
            doc.append("doors", (int) Math.round((double) data.get("doors")))
                    .append("airConditioned", data.get("airConditioned"))
                    .append("trunkCapacity", (int) Math.round((double) data.get("trunkCapacity")))
                    .append("type", "car");
        } else if (((String) data.get("type")).equalsIgnoreCase("bike")) {
            doc.append("wheelSize", data.get("wheelSize"))
                    .append("sideCar", data.get("sideCar"))
                    .append("numOfHelmets", (int) Math.round((double) data.get("numOfHelmets")))
                    .append("type", "bike");
        } else{
            throw new IllegalArgumentException(String.format("%s is not a valid Vehicle Type", data.get("type")));
        }

        return doc;
    }
}
