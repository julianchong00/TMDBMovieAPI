package Controller;

import Helper.DatabaseController;
import Model.ProfileModel;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TMDBView extends Application {

    @FXML private ComboBox<String> imageSizeComboBox;
    @FXML private Button submitButton;
    @FXML private Button deleteButton;
    @FXML private TableView<ProfileModel> dataTable;
    @FXML private TableColumn<ProfileModel, Integer> col_id;
    @FXML private TableColumn<ProfileModel, String> col_name;
    @FXML private TableColumn<ProfileModel, String> col_description;
    @FXML private TableColumn<ProfileModel, String> col_imageSize;
    @FXML private TableColumn<ProfileModel, String> col_filename;
    @FXML private TableColumn<ProfileModel, String> col_link;
    @FXML private Spinner<Integer> idSpinner;
    @FXML private ImageView profileImageView;
    @FXML private Label profileNameLabel;
    @FXML private Label originalLinkLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label fileNameLabel;

    private Stage stage;
    private ObservableList<ProfileModel> profileList = FXCollections.observableArrayList();

    private static final String API_KEY = "95f7a06840a48ea44951675cc85f4838";
    private static final String GET_PERSON_BASE_URL = "https://api.themoviedb.org";
    private static final String GET_IMAGE_BASE_URL = "https://image.tmdb.org/t/p";

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/tmdbUI.fxml"));
        Scene scene = new Scene(root, 800, 650);
        stage.setMinWidth(800);
        stage.setMinHeight(650);
        stage.setTitle("TMDB Movie Database");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void initialize() {
        DatabaseController.createTable();

        try {
            Connection con = DatabaseController.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM Profiles");
            while (rs.next()) {
                profileList.add(new ProfileModel(rs.getInt("id"), rs.getString("title"),
                        rs.getString("description"), rs.getString("imageSize"),
                        rs.getString("filename"), rs.getString("link")));
            }
            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        col_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        col_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        col_description.setCellValueFactory(new PropertyValueFactory<>("description"));
        col_imageSize.setCellValueFactory(new PropertyValueFactory<>("imageSize"));
        col_filename.setCellValueFactory(new PropertyValueFactory<>("filename"));
        col_link.setCellValueFactory(new PropertyValueFactory<>("link"));

        dataTable.setItems(profileList);

        imageSizeComboBox.getItems().addAll("w45",
                "w185",
                "h632",
                "original");
        imageSizeComboBox.getSelectionModel().selectFirst();

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        idSpinner.setValueFactory(valueFactory);

        dataTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                viewSelectedProfile(newSelection);
            }
        });
    }

    @FXML
    private void handleSubmitButton(ActionEvent event) throws IOException {
        JSONObject json = new JSONObject(retrievePhoto());
        if (!json.isEmpty()) {
            Integer id = (Integer) json.get("id");
            String name = (String) json.get("name");
            String description = (String) json.get("biography");
            if (description.isEmpty()) {
                description = String.format("No description found for %s", name);
            }
            String imageSize = imageSizeComboBox.getSelectionModel().getSelectedItem();
            String profilePath = "";
            String link = "";
            if (!json.isNull("profile_path")) {
                profilePath = (String) json.get("profile_path");
                link = GET_IMAGE_BASE_URL + "/" + imageSize + profilePath;
            } else {
                profilePath = String.format("No image found for %s", name);
                link = String.format("Image path does not exist on TMDB for %s", name);
            }

            ProfileModel profile = new ProfileModel(id, name, description, imageSize, profilePath, link);
            System.out.println(profileList.contains(profile));
            if (!profileList.contains(profile)) {
                DatabaseController.insertRecord(id, name, description, profilePath, link, imageSize);
                profileList.add(profile);
            }
        }
    }

    @FXML
    private void handleDeleteButton(ActionEvent event) {
        ProfileModel profile = dataTable.getSelectionModel().getSelectedItem();
        if (profile != null) {
            DatabaseController.deleteRecord(profile.getId(), profile.getImageSize());
            profileList.remove(profile);
            clearLabels();
            dataTable.getSelectionModel().clearSelection();
        }
    }

    private void viewSelectedProfile(ProfileModel selectedProfile) {
        if (selectedProfile != null) {
            clearLabels();
            profileNameLabel.setText(selectedProfile.getName());
            descriptionLabel.setText(selectedProfile.getDescription());
            fileNameLabel.setText(selectedProfile.getFilename());
            originalLinkLabel.setText(selectedProfile.getLink());
            if (validateLink(selectedProfile.getLink())) {
                Image image = new Image(selectedProfile.getLink());
                profileImageView.setImage(image);
            }
        }
    }

    private boolean validateLink(String link) {
        String urlRegex = "((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(link);
        return matcher.find();
    }

    private String retrievePhoto() throws IOException {
        String id = idSpinner.getValue().toString();
        String urlString = GET_PERSON_BASE_URL + "/3/person/" + id + "?api_key=" + API_KEY;

        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            return response.toString();
        } else {
            System.out.println("GET request failed");
            return "{}";
        }
    }

    private void clearLabels() {
        profileNameLabel.setText("");
        descriptionLabel.setText("");
        fileNameLabel.setText("");
        originalLinkLabel.setText("");
        profileImageView.setImage(null);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
