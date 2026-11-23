import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherService {

    public static void main(String[] args) {
        String apiKey = "773328b1-51c3-4007-aa2c-cee6824773c8";

        double lat = 49;
        double lon = 40;
        int limit = 3;

        String url = "https://api.weather.yandex.ru/v2/forecast?lat=" + lat + "&lon=" + lon + "&limit=" + limit;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Yandex-Weather-Key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Ошибка при запросе к API. Код ответа: " + response.statusCode());
                System.out.println("Тело ответа: " + response.body());
                return;
            }

            String jsonResponse = response.body();

            System.out.println("Полный ответ от сервиса:");
            System.out.println(jsonResponse);
            System.out.println(" ");

            int currentTemp = extractFactTemp(jsonResponse);
            System.out.println("Текущая температура (fact.temp): " + currentTemp + " °C");

            double avgForecastTemp = calculateAverageTemp(jsonResponse, limit);
            System.out.printf("Средняя дневная температура по прогнозу за %d дн.: %.2f °C%n", limit, avgForecastTemp);

        } catch (Exception e) {
            System.out.println("Произошла ошибка при выполнении программы: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int extractFactTemp(String json) {
        int factIndex = json.indexOf("\"fact\"");
        if (factIndex == -1) {
            throw new RuntimeException("Блок 'fact' не найден в ответе");
        }

        String tempKey = "\"temp\":";
        int tempKeyIndex = json.indexOf(tempKey, factIndex);
        int valueStart = tempKeyIndex + tempKey.length();

        int commaIndex = json.indexOf(",", valueStart);
        int braceIndex = json.indexOf("}", valueStart);

        int valueEnd;
        if (commaIndex != -1 && (braceIndex == -1 || commaIndex < braceIndex)) {
            valueEnd = commaIndex;
        } else {
            valueEnd = braceIndex;
        }

        String tempStr = json.substring(valueStart, valueEnd).trim();
        return Integer.parseInt(tempStr);
    }

    private static double calculateAverageTemp(String json, int limit) {
        int searchFromIndex = json.indexOf("\"forecasts\"");
        if (searchFromIndex == -1) {
            throw new RuntimeException("Блок 'forecasts' не найден в ответе");
        }

        double sum = 0;
        int count = 0;

        for (int i = 0; i < limit; i++) {
            int dayIndex = json.indexOf("\"day\":", searchFromIndex);
            if (dayIndex == -1) {
                break;
            }

            String avgKey = "\"temp_avg\":";
            int avgKeyIndex = json.indexOf(avgKey, dayIndex);
            if (avgKeyIndex == -1) {
                break;
            }

            int valueStart = avgKeyIndex + avgKey.length();

            int commaIndex = json.indexOf(",", valueStart);
            int braceIndex = json.indexOf("}", valueStart);

            int valueEnd;
            if (commaIndex != -1 && (braceIndex == -1 || commaIndex < braceIndex)) {
                valueEnd = commaIndex;
            } else {
                valueEnd = braceIndex;
            }

            String avgStr = json.substring(valueStart, valueEnd).trim();
            int dayAvgTemp = Integer.parseInt(avgStr);

            sum += dayAvgTemp;
            count++;

            searchFromIndex = valueEnd;
        }

        if (count == 0) {
            throw new RuntimeException("Не удалось извлечь данные о температуре прогноза");
        }

        return sum / count;
    }
}