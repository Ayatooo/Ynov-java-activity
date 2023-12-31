package fr.ayato.activity.gui;

import com.mongodb.client.MongoCollection;
import fr.ayato.activity.Connection;
import fr.ayato.activity.controller.ActivityControllerImpl;
import fr.ayato.activity.controller.CalculControllerImpl;
import fr.ayato.activity.model.ActivityDTO;
import fr.ayato.activity.repository.ActivityRepositoryImpl;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static fr.ayato.activity.mapper.ActivityMapper.documentToActivity;

@Slf4j
public class ActivityListWindow extends JFrame {

    Dotenv dotenv = Dotenv.configure().load();
    private final ActivityControllerImpl activityController;
    private boolean ascendingOrder = true;
    private final CalculControllerImpl calculControllerImpl;

    public ActivityListWindow() {
        // On initialise la fenêtre
        super("Liste des activités ✨");
        calculControllerImpl = new CalculControllerImpl();
        MongoCollection<Document> collection = Connection.client(this.dotenv.get("DB_NAME"), this.dotenv.get("DB_COLLECTION_ACT"));
        ActivityRepositoryImpl activityRepository = new ActivityRepositoryImpl(collection);
        this.activityController = new ActivityControllerImpl(activityRepository);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textPane);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Bouton pour actualiser la liste des activités / l'afficher
        JButton refreshButton = new JButton("Actualiser");
        refreshButton.addActionListener(e -> refreshActivityList(textPane));

        // Bouton pour revenir à la page d'accueil
        JButton buttonBack = new JButton("Retour");
        buttonBack.addActionListener(e -> {
            new HomeWindow();
            dispose();
        });

        // Bouton pour trier les activités par date (un clic = tri ascendant, un autre clic = tri descendant)
        JButton sortButton = new JButton("Trier par date");
        sortButton.addActionListener(e -> sortActivitiesByDate(textPane));

        // Bouton pour créer une nouvelle activité
        JButton addButton = new JButton("Créer");
        addButton.addActionListener(e -> {
            dispose();
            new CreateActivityWindow();
        });

        // Bouton pour filtrer les activités par semaine de l'année
        JButton filterButton = new JButton("Filtrer par semaine");
        filterButton.addActionListener(e -> filterActivitiesByWeek(textPane));

        // On ajoute les boutons au panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(refreshButton);
        buttonPanel.add(sortButton);
        buttonPanel.add(addButton);
        buttonPanel.add(filterButton);
        buttonPanel.add(buttonBack);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        refreshActivityList(textPane);
        setVisible(true);
    }

    // On récupère la liste des activités et on l'affiche dans le JTextPane
    private void refreshActivityList(JTextPane textPane) {
        textPane.setBackground(Color.WHITE);
        textPane.setText("");
        try {
            List<ActivityDTO> activityDTOList = getActivityList();
            displayActivities(textPane, activityDTOList);
        } catch (Exception e) {
            log.error("Failed to fetch activity list: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur lors de la récupération de la liste des activités.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // On récupère la liste des activités par call à la BDD
    private List<ActivityDTO> getActivityList() {
        MongoCollection<Document> activities = activityController.getAll();
        List<ActivityDTO> activityDTOList = new ArrayList<>();
        for (Document activity : activities.find()) {
            activityDTOList.add(documentToActivity(activity));
        }
        return activityDTOList;
    }

    // On affiche les activités dans le JTextPane
    private void displayActivities(JTextPane textPane, List<ActivityDTO> activityDTOList) {
        StringBuilder sb = new StringBuilder();
        for (ActivityDTO activityDTO : activityDTOList) {
            sb.append("Activité : ").append(activityDTO.getName()).append("\n")
                    .append("Durée : ").append(activityDTO.getDuration()).append("\n")
                    .append("RPE : ").append(activityDTO.getRpe()).append("\n")
                    .append("Charge : ").append(activityDTO.getCharge()).append("\n")
                    .append("Date : ").append(activityDTO.getDate()).append("\n")
                    .append("------------------------------------------------------\n");
        }
        textPane.setText(sb.toString());
    }

    // On trie les activités par date
    private void sortActivitiesByDate(JTextPane textPane) {
        textPane.setBackground(Color.WHITE);
        textPane.setText("");
        try {
            List<ActivityDTO> activityDTOList = getActivityList();
            sortActivitiesByDate(activityDTOList);
            displayActivities(textPane, activityDTOList);
        } catch (Exception e) {
            log.error("Failed to sort activity list: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur lors du tri de la liste des activités.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // On ordonne les activités par date (ascendant ou descendant) → et on inverse l'ordre à chaque clic
    private void sortActivitiesByDate(List<ActivityDTO> activityDTOList) {
        if (ascendingOrder) {
            activityDTOList.sort(Comparator.comparing(ActivityDTO::getDate));
        } else {
            activityDTOList.sort(Comparator.comparing(ActivityDTO::getDate).reversed());
        }
        ascendingOrder = !ascendingOrder;
    }

    // On filtre les activités par semaine de l'année
    private void filterActivitiesByWeek(JTextPane textPane) {
        try {
            List<Integer> weekNumbers = new ArrayList<>();
            for (int i = 1; i <= 52; i++) {
                weekNumbers.add(i);
            }

            // On affiche une liste déroulante pour sélectionner le numéro de semaine
            Integer selectedWeekNumber = (Integer) JOptionPane.showInputDialog(this,
                    "Sélectionnez le numéro de semaine :", "Filtrer par semaine 🚩",
                    JOptionPane.QUESTION_MESSAGE, null, weekNumbers.toArray(), weekNumbers.get(0));

            // Si on a sélectionné une semaine, on filtre les activités
            if (selectedWeekNumber != null) {

                List<ActivityDTO> filteredList;
                filteredList = filterActivitiesByWeekNumber(selectedWeekNumber);

                log.info("Filtering activities by week...");

                // On récupère les 4 dernières semaines
                // Obtenir les numéros de semaine des trois semaines précédentes
                List<Integer> fourWeekNumbers = new ArrayList<>();
                for (int i = selectedWeekNumber - 3; i <= selectedWeekNumber; i++) {
                    if (i > 0) {
                        fourWeekNumbers.add(i);
                    }
                }
                List<ActivityDTO> last4Weeks = new ArrayList<>();
                for (Integer week : fourWeekNumbers) {
                    List<ActivityDTO> weekActivities = filterActivitiesByWeekNumber(week);
                    last4Weeks.addAll(weekActivities);
                }
                // On affiche les activités filtrées
                StringBuilder sb = new StringBuilder();
                if (filteredList.isEmpty()) {
                    sb.append("Aucune activité ❌");
                } else {
                    displayActivities(textPane, filteredList);

                    int chargeAigue = this.calculControllerImpl.calculateChargeAigue(filteredList);
                    int chargeChronique = this.calculControllerImpl.calculateChargeAigue(last4Weeks);
                    double acwr = this.calculControllerImpl.calculateAcwr(chargeAigue, chargeChronique);
                    List<ActivityDTO> formattedWeekTrain = this.calculControllerImpl.formattedWeekTrain(filteredList);
                    double monotonie = this.calculControllerImpl.calculateMonotony(filteredList, formattedWeekTrain);
                    double averageDailyTrainingLoad = this.calculControllerImpl.calculateAverageLoad(filteredList);
                    double constraint = this.calculControllerImpl.calculateConstraint(chargeAigue, monotonie);
                    double fitness = this.calculControllerImpl.calculateFitness(chargeAigue, constraint);
                    ArrayList<String> healthIndicator = this.calculControllerImpl.calculateHealthIndicator(monotonie, constraint, acwr);
                    switch (healthIndicator.get(0)) {
                        case "green" -> textPane.setBackground(Color.GREEN);
                        case "orange" -> textPane.setBackground(Color.ORANGE);
                        case "red" -> textPane.setBackground(Color.RED);
                        case "blue" -> textPane.setBackground(Color.BLUE);
                        default -> textPane.setBackground(Color.WHITE);
                    }
                    sb.append("\n");
                    sb.append("Forme : ").append(healthIndicator.get(1)).append("\n");
                    sb.append("\n");
                    sb.append("Charge Aigue: ").append(chargeAigue).append("\n");
                    sb.append("Charge chronique: ").append(chargeChronique).append("\n");
                    sb.append("ACWR: ").append(acwr).append("\n");
                    sb.append("Monotonie: ").append(monotonie).append("\n");
                    sb.append("Contrainte: ").append(constraint).append("\n");
                    sb.append("Fitness: ").append(fitness).append("\n");
                    sb.append("Charge moyenne quotidienne: ").append(averageDailyTrainingLoad).append("\n");
                }
                // On affiche les résultats dans une boîte de dialogue
                JOptionPane.showMessageDialog(this, sb.toString(), "Résultats", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Failed to filter activity list: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur lors du filtrage de la liste des activités.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<ActivityDTO> filterActivitiesByWeekNumber(int weekNumber) {
        List<ActivityDTO> activityDTOList = getActivityList();
        List<ActivityDTO> filteredList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.WEEK_OF_YEAR, weekNumber);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_WEEK, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        // Filtrer les activités par date
        for (ActivityDTO activityDTO : activityDTOList) {
            Date activityDate = activityDTO.getDate();
            if (activityDate.after(startDate) && activityDate.before(endDate)) {
                filteredList.add(activityDTO);
            }
        }

        return filteredList;
    }
}
