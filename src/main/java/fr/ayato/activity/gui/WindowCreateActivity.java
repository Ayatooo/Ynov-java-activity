package fr.ayato.activity.gui;

import com.mongodb.client.MongoCollection;
import fr.ayato.activity.Connection;
import fr.ayato.activity.controller.ActivityControllerImpl;
import fr.ayato.activity.enums.Effort;
import fr.ayato.activity.model.ActivityDTO;
import fr.ayato.activity.repository.ActivityRepositoryImpl;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

@Slf4j
public class WindowCreateActivity extends JFrame {
    public WindowCreateActivity(){
        super("Petite fenêtre bien sympa");
        Toolkit tk = Toolkit.getDefaultToolkit();
        int screenHeightSize = tk.getScreenSize().height;
        int screenWidthSize = tk.getScreenSize().width;

        setBounds(0,0, screenWidthSize/2, screenHeightSize/2);
        setLocationRelativeTo(null);

        Container contentPane = getContentPane();

        ActivityForm activityForm = new ActivityForm();
        for (Effort effort : Effort.values()) {
            activityForm.getComboBoxRpe().addItem(effort);
        }
        activityForm.getValiderButton().setActionCommand("Valider");
        activityForm.getValiderButton().addActionListener(new ButtonFormListner(activityForm));
        activityForm.getFermerButton().setActionCommand("Fermer");
        activityForm.getFermerButton().addActionListener(new ButtonListner());


        contentPane.add(activityForm.getRootPanel(), BorderLayout.CENTER);


        setVisible(true);
    }

    class ButtonListner implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(e.getActionCommand().equals("Fermer")){
                System.exit(0);
            }
        }
    }

    class ButtonFormListner implements ActionListener {
        Dotenv dotenv = Dotenv.configure().load();
        ActivityControllerImpl activityController;
        ActivityRepositoryImpl activityRepository;
        MongoCollection<Document> collection;
        ActivityForm activityForm;
        private String name;
        private int duration;
        private Date date;
        private int rpe;
        private int marge;
        public ButtonFormListner(ActivityForm activityForm) {
            this.collection = Connection.client(this.dotenv.get("DB_NAME"), this.dotenv.get("DB_COLLECTION"));
            this.activityRepository = new ActivityRepositoryImpl(this.collection);
            this.activityController = new ActivityControllerImpl(this.activityRepository);
            this.activityForm = activityForm;
        }
        public void actionPerformed(ActionEvent e) {
            this.name = this.activityForm.getTextFieldName().getText();
            this.duration = (int)this.activityForm.getSpinnerDuration().getValue();
            this.date = new Date();
            this.rpe = this.activityForm.getComboBoxRpe().getSelectedIndex();
            this.marge = this.rpe * this.duration;

            ActivityDTO activity = new ActivityDTO(
                    this.name,
                    this.duration,
                    new Date(),
                    this.rpe,
                    this.marge
            );
            String id = activityController.saveActivity(activity);
            log.info("Actvity created", id);

            JDialog dialog = new JDialog();
            JPanel panel = new JPanel();
            JLabel label = new JLabel("Activité créée avec succès");
            JButton button = new JButton("Fermer");
            button.setActionCommand("Fermer");
            button.addActionListener(new ButtonDialogListner(dialog));
            panel.add(label);
            panel.add(button);
            dialog.add(panel);
            dialog.setSize(200, 100);
            dialog.setVisible(true);
        }
    }

    class ButtonDialogListner implements ActionListener {
        JDialog dialog;
        public ButtonDialogListner(JDialog dialog) {
            this.dialog = dialog;
        }
        public void actionPerformed(ActionEvent e) {
            if(e.getActionCommand().equals("Fermer")){
                this.dialog.dispose();
                JFrame window = new WindowCreateActivity();
            }
        }
    }

    public static void main(String[] args) {
        JFrame window = new WindowCreateActivity();
    }
}