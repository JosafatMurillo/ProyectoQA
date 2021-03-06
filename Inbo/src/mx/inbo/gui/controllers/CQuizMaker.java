/*
 * Nombre de la clase:  CQuizMaker.java
 * Versión:             1.0
 * Fecha:               27/10/2018
 *
 * Copyright 2018 Adolf Ángel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mx.inbo.gui.controllers;

import animatefx.animation.BounceInLeft;
import animatefx.animation.SlideInLeft;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mx.inbo.domain.KeyGenerator;
import mx.inbo.domain.Thumbnail;
import mx.inbo.entities.Quiz;
import mx.inbo.gui.tools.FileHelper;
import mx.inbo.gui.tools.Loader;
import mx.inbo.gui.tools.Mensaje;

/**
 * Clase FXML controladora de la página Quiz Maker.
 *
 * @author adolf
 */
public class CQuizMaker implements Initializable {

    private static Quiz quiz;

    public static void setQuiz(Quiz quizz) {
        quiz = quizz;
    }

    @FXML
    private BorderPane mainPane;

    @FXML
    private StackPane thumbnailPane;

    @FXML
    private StackPane centerPane;

    @FXML
    private StackPane infoStack;

    @FXML
    private ImageView thumbnail;

    @FXML
    private JFXButton backButton;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionField;

    private File imageFile;
    private ResourceBundle bundle;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        mainPane.widthProperty().addListener((objects, oldValue, newValue) -> {
            double width = (double) newValue / 2;
            thumbnail.setFitWidth(width - 10);
            thumbnailPane.setPrefWidth(width);
            centerPane.setPrefSize(width, mainPane.getHeight());

            double infoWidth = width - 20;
            infoStack.setPrefWidth(infoWidth);
            titleField.setPrefWidth(infoWidth);
            descriptionField.setPrefWidth(infoWidth);
        });

        playIntroAnimation();

        if (quiz != null) {
            titleField.setText(quiz.getTitulo());
            descriptionField.setText(quiz.getDescripcion());
            Thumbnail thumb = quiz.getImage();
            Image image = new Image(new ByteArrayInputStream(thumb.getImage()));
            thumbnail.setImage(image);
        } else {
            quiz = new Quiz();
        }
        
        bundle = rb;

    }

    /**
     * Reproduce la animación de inicio.
     */
    private void playIntroAnimation() {

        new BounceInLeft(mainPane).play();
        new SlideInLeft(thumbnailPane).play();

    }

    /**
     * Regresa a la página anterior.
     */
    @FXML
    private void stepBack() {
        Stage actualStage = (Stage) backButton.getScene().getWindow();
        Loader.loadPageInCurrentStage("/mx/inbo/gui/Dashboard.fxml", "Dashboard", actualStage);
    }

    /**
     * Avanza al siguiente paso del registro.
     */
    @FXML
    private void nextPage() {

        String title = titleField.getText();
        String description = descriptionField.getText();

        Thumbnail thumb = quiz.getImage();

        if (thumb == null) {

            thumb = new Thumbnail();
            imageFile = new File(System.getProperty("user.dir") + "/src/mx/inbo/images/default_thumbnail.png");
            thumb.setType("Quiz");
        }

        if (imageFile != null) {
            int extIndex = imageFile.getName().lastIndexOf('.');
            String imageExtention = imageFile.getName().substring(extIndex + 1).toLowerCase();
            thumb.setExtention(imageExtention);

            byte[] image = FileHelper.parseFileToBytes(imageFile, imageExtention);
            thumb.setImage(image);

            quiz.setImagen(imageFile.toURI().toString());
        }

        if (!CQuizQuestions.gonnaEdit()) {
            int id = KeyGenerator.obtenerId();

            quiz.setIdQuiz(id);
        }
        quiz.setIdUser(CDashboard.getUser());
        quiz.setImage(thumb);

        if (!title.isEmpty() && !description.isEmpty()) {
            quiz.setTitulo(title);
            quiz.setDescripcion(description);

            Stage actualStage = (Stage) backButton.getScene().getWindow();
            CQuizQuestions.setQuiz(quiz);
            CQuizQuestions.setStage(actualStage);
            Loader.loadPageInCurrentStage("/mx/inbo/gui/QuizQuestions.fxml", "Questions", actualStage);
        }else{
            Mensaje alerta = new Mensaje();
            alerta.setHeader(bundle.getString("key.emptyFieldTitle"));
            alerta.setBody(bundle.getString("key.emptyField"));
            JFXDialog dialog = new JFXDialog(centerPane, alerta, JFXDialog.DialogTransition.CENTER);

            dialog.show();
        }

    }

    /**
     * Cambia la imagen de portada del Quiz.
     */
    @FXML
    private void changeImage() {

        Stage actualStage = (Stage) backButton.getScene().getWindow();

        FileChooser chooser = new FileChooser();

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.*"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );

        imageFile = chooser.showOpenDialog(actualStage);

        Image image = new Image(imageFile.toURI().toString());

        thumbnail.setImage(image);
    }

}
