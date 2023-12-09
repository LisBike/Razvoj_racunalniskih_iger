package si.um.feri.game.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Stations  {
    private URL url;
    public Array<Station> stationArray = new Array<>();

    public Stations(String url) throws MalformedURLException {
        this.url = new URL(url);
        this.refresh();
    }

    public void refresh() {
        try {
            HttpURLConnection req = (HttpURLConnection) url.openConnection();
            req.setRequestMethod("GET");
            req.connect();

            int res_code = req.getResponseCode();
            if (res_code != 200) throw new RuntimeException("Response code: " + res_code);
            else {
                String lines;
                StringBuilder response = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
                while ((lines = reader.readLine()) != null) response.append(lines);
                Json json = new Json();
                stationArray = json.fromJson(Array.class, Station.class, response.toString());
//                for (Station data : stationArray) {
//                    double latitude = data.latitude;
//                    double longitude = data.longitude;
//
//                    System.out.println("Latitude: " + latitude + ", Longitude: " + longitude);
//                }

                reader.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(url.toString());
        }
    }
}
