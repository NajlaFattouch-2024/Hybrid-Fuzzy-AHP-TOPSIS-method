package fuzzy2;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class fuzzyTopsis2 extends JFrame {

    private final int numObjects = 21; // Nombre d'objets à évaluer
    private final int numCriteria = 7; // Nombre de critères
    private final double[][][] fuzzyDecisionMatrix;
    private final double[][][] normalizedMatrix; // Matrice normalisée
    private final double[][][] weightedNormalizedMatrix; // Matrice pondérée floue
    private final double[][] fuzzyWeights; // Poids sous forme floue
    private JCheckBox[] maximizeCriteria; // Case à cocher pour chaque critère
    
    double[] distancePositive = new double[numObjects];
    double[] distanceNegative = new double[numObjects];
    double[] idealPositive ;
    double[] idealNegative ;
    double[] closenessCoefficients;


    // Valeurs extrêmes des critères (comme dans votre exemple)
    private final double[] uMin = {0, 1, 1,0, 0, 1, 0}; // Pour les critères de minimisation
    private final double[] uMax = {6, 0, 0, 5, 7, 0, 7}; // Pour les critères de maximisation

    public fuzzyTopsis2() {
        this.fuzzyDecisionMatrix = new double[numObjects][numCriteria][3];
        this.normalizedMatrix = new double[numObjects][numCriteria][3];
        this.weightedNormalizedMatrix = new double[numObjects][numCriteria][3];
        this.fuzzyWeights = new double[numCriteria][3]; // Poids flous

        setTitle("Fuzzy TOPSIS for Selecting Cultural Heritage Items");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Enter your fuzzy values ​​for each criterion and object :", JLabel.CENTER), BorderLayout.NORTH);

        JPanel matrixPanel = new JPanel(new GridLayout(numObjects + 2, numCriteria + 1)); // +2 pour ajouter la ligne des poids
        JTextField[][][] fields = new JTextField[numObjects + 1][numCriteria][3]; // +1 pour la ligne des poids

        // Initialisation des cases à cocher
        maximizeCriteria = new JCheckBox[numCriteria];
        JPanel checkBoxPanel = new JPanel(new GridLayout(1, numCriteria));
        for (int j = 0; j < numCriteria; j++) {
            maximizeCriteria[j] = new JCheckBox("Maximize C" + (j + 1));
            checkBoxPanel.add(maximizeCriteria[j]);
        }
        panel.add(checkBoxPanel, BorderLayout.NORTH);

        // Reste de la matrice
        for (int i = 0; i <= numObjects + 1; i++) { // +1 pour la ligne des poids
            for (int j = 0; j <= numCriteria; j++) {
                if (i == 0 && j == 0) {
                    matrixPanel.add(new JLabel("")); // Case vide en haut à gauche
                } else if (i == 0) {
                    matrixPanel.add(new JLabel("C" + j, JLabel.CENTER)); // Labels des colonnes (Critères)
                } else if (j == 0) {
                    if (i == numObjects + 1) { // Ligne des poids
                        matrixPanel.add(new JLabel("Weight", JLabel.CENTER));
                    } else {
                        matrixPanel.add(new JLabel("O" + i, JLabel.CENTER)); // Labels des lignes (Objets)
                    }
                } else {
                    JPanel fuzzyFieldPanel = new JPanel(new GridLayout(1, 3));
                    for (int k = 0; k < 3; k++) {
                        JTextField field = new JTextField();
                        fields[i - 1][j - 1][k] = field;
                        fuzzyFieldPanel.add(field);
                    }
                    matrixPanel.add(fuzzyFieldPanel);
                }
            }
        }
        panel.add(matrixPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton validateButton = new JButton("Validate");
        validateButton.addActionListener(e -> {
            if (saveFuzzyDecisionMatrix(fields)) {
                normalizeMatrix();
                displayNormalizedMatrix();
                calculateWeightedNormalizedMatrix(); // Calcul de la matrice pondérée
                displayWeightedNormalizedMatrix(); // Affichage de la matrice pondérée
                calculateIdealSolutions();
                calculateDistances(idealPositive, idealNegative);
                displayDistanceResults();
                calculateClosenessCoefficients();
                displayClosenessCoefficients (closenessCoefficients);
                rankObjectsByCloseness ();
            }
        });
        buttonPanel.add(validateButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        add(panel);
    }

    private boolean saveFuzzyDecisionMatrix(JTextField[][][] fields) {
        // Sauvegarde des poids des critères dans la matrice des poids
        for (int j = 0; j < numCriteria; j++) {
            try {
                String value = fields[numObjects][j][0].getText().trim(); // Récupère le champ de poids pour Cj
                if (value.isEmpty()) {
                    throw new NumberFormatException("Empty field detected for weights.");
                }
                fuzzyWeights[j][0] = Double.parseDouble(value); // l
                value = fields[numObjects][j][1].getText().trim(); // Récupère le champ pour m
                fuzzyWeights[j][1] = Double.parseDouble(value); // m
                value = fields[numObjects][j][2].getText().trim(); // Récupère le champ pour u
                fuzzyWeights[j][2] = Double.parseDouble(value); // u
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                        "Error in fields for C weights" + (j + 1) + ": " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        // Sauvegarde de la matrice de décision floue
        for (int i = 0; i < numObjects; i++) {
            for (int j = 0; j < numCriteria; j++) {
                try {
                    for (int k = 0; k < 3; k++) {
                        String value = fields[i][j][k].getText().trim(); // Supprimez les espaces superflus
                        if (value.isEmpty()) {
                            throw new NumberFormatException("Empty field.");
                        }
                        fuzzyDecisionMatrix[i][j][k] = Double.parseDouble(value);
                    }
                    if (!isTriangularValid(fuzzyDecisionMatrix[i][j])) {
                        throw new IllegalArgumentException("The values ​​do not form a valid triangular fuzzy number.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, 
                            "Error in the field of O" + (i + 1) + " et C" + (j + 1) + ": " + ex.getMessage(), 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, 
                            "Error in the field of O" + (i + 1) + " et C" + (j + 1) + ": " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTriangularValid(double[] fuzzyValue) {
        // Vérifie si les valeurs forment un nombre flou triangulaire valide
        return fuzzyValue[0] <= fuzzyValue[1] && fuzzyValue[1] <= fuzzyValue[2];
    }

    private void normalizeMatrix() {
        // Normalisation selon les valeurs extrêmes des critères
        for (int i = 0; i < numObjects; i++) {
            for (int j = 0; j < numCriteria; j++) {
                if (maximizeCriteria[j].isSelected()) { // Critère à maximiser
                    normalizedMatrix[i][j][0] = fuzzyDecisionMatrix[i][j][0] / uMax[j]; // l
                    normalizedMatrix[i][j][1] = fuzzyDecisionMatrix[i][j][1] / uMax[j]; // m
                    normalizedMatrix[i][j][2] = fuzzyDecisionMatrix[i][j][2] / uMax[j]; // u
                } else { // Critère à minimiser
                    normalizedMatrix[i][j][0] = uMin[j] / fuzzyDecisionMatrix[i][j][2]; // l
                    normalizedMatrix[i][j][1] = uMin[j] / fuzzyDecisionMatrix[i][j][1]; // m
                    normalizedMatrix[i][j][2] = uMin[j] / fuzzyDecisionMatrix[i][j][0]; // u
                }
            }
        }
    }

    private void displayNormalizedMatrix() {
        StringBuilder sb = new StringBuilder("Normalized Fuzzy Matrix :\n");
        for (int i = 0; i < numObjects; i++) {
            for (int j = 0; j < numCriteria; j++) {
                sb.append("O" + (i + 1) + " - C" + (j + 1) + ": ");
                sb.append("(" + normalizedMatrix[i][j][0] + ", ");
                sb.append(normalizedMatrix[i][j][1] + ", ");
                sb.append(normalizedMatrix[i][j][2] + ")\n");
            }
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Normalized Fuzzy Matrix", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calculateWeightedNormalizedMatrix() {
        // Calcul de la matrice pondérée
        for (int i = 0; i < numObjects; i++) {
            for (int j = 0; j < numCriteria; j++) {
                for (int k = 0; k < 3; k++) {
                    weightedNormalizedMatrix[i][j][k] = normalizedMatrix[i][j][k] * fuzzyWeights[j][k];
                }
            }
        }
    }

    private void displayWeightedNormalizedMatrix() {
        StringBuilder sb = new StringBuilder("Normalized Weighted Matrix:\n");
        for (int i = 0; i < numObjects; i++) {
            for (int j = 0; j < numCriteria; j++) {
                sb.append("O" + (i + 1) + " - C" + (j + 1) + ": ");
                sb.append("(" + weightedNormalizedMatrix[i][j][0] + ", ");
                sb.append(weightedNormalizedMatrix[i][j][1] + ", ");
                sb.append(weightedNormalizedMatrix[i][j][2] + ")\n");
            }
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Normalized Weighted Matrix", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    //*****
    
    private void calculateIdealSolutions() {
        // Initialisation des solutions idéales
        idealPositive = new double[numCriteria * 3]; // Contient A+ (pour chaque critère et ses 3 composantes)
        idealNegative = new double[numCriteria * 3]; // Contient A- (pour chaque critère et ses 3 composantes)

        // Calcul des solutions idéales
        for (int j = 0; j < numCriteria; j++) {
            double maxL = Double.MIN_VALUE, maxM = Double.MIN_VALUE, maxU = Double.MIN_VALUE;
            double minL = Double.MAX_VALUE, minM = Double.MAX_VALUE, minU = Double.MAX_VALUE;

            // Parcourir les objets pour trouver les valeurs maximales et minimales
            for (int i = 0; i < numObjects; i++) {
                // Valeurs des composantes pour l'objet i et critère j
                double l = weightedNormalizedMatrix[i][j][0];
                double m = weightedNormalizedMatrix[i][j][1];
                double u = weightedNormalizedMatrix[i][j][2];

                // Calcul des valeurs maximales et minimales
                if (l > maxL) maxL = l;
                if (m > maxM) maxM = m;
                if (u > maxU) maxU = u;

                if (l < minL) minL = l;
                if (m < minM) minM = m;
                if (u < minU) minU = u;
            }

            // Sauvegarder les résultats dans les tableaux des solutions idéales
            idealPositive[j * 3] = maxL;
            idealPositive[j * 3 + 1] = maxM;
            idealPositive[j * 3 + 2] = maxU;

            idealNegative[j * 3] = minL;
            idealNegative[j * 3 + 1] = minM;
            idealNegative[j * 3 + 2] = minU;
        }

        // Affichage des résultats
        displayIdealSolutions(idealPositive, idealNegative);
    }
    
    
    private void displayIdealSolutions(double[] idealPositive, double[] idealNegative) {
        StringBuilder sb = new StringBuilder("Ideal solutions (A+ et A-):\n");

        for (int j = 0; j < numCriteria; j++) {
            sb.append("Criterion C" + (j + 1) + ":\n");
            sb.append("A+ (Positive ideal solution): ");
            sb.append("(" + idealPositive[j * 3] + ", ");
            sb.append(idealPositive[j * 3 + 1] + ", ");
            sb.append(idealPositive[j * 3 + 2] + ")\n");

            sb.append("A- (Negative ideal solution): ");
            sb.append("(" + idealNegative[j * 3] + ", ");
            sb.append(idealNegative[j * 3 + 1] + ", ");
            sb.append(idealNegative[j * 3 + 2] + ")\n");
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "Ideal solutions", JOptionPane.INFORMATION_MESSAGE);
    }
    //****

    private void calculateDistances(double[] idealPositive, double[] idealNegative) {
        for (int i = 0; i < numObjects; i++) {
            double distanceToPositive = 0;
            double distanceToNegative = 0;

            for (int j = 0; j < numCriteria; j++) {
                // Récupération des composantes des valeurs pondérées et des solutions idéales
                double[] weightedValue = weightedNormalizedMatrix[i][j];
                double[] positiveIdeal = {idealPositive[j * 3], idealPositive[j * 3 + 1], idealPositive[j * 3 + 2]};
                double[] negativeIdeal = {idealNegative[j * 3], idealNegative[j * 3 + 1], idealNegative[j * 3 + 2]};

                // Calcul de la distance floue (par exemple, somme des écarts absolus)
                distanceToPositive += calculateFuzzyDistance(weightedValue, positiveIdeal);
                distanceToNegative += calculateFuzzyDistance(weightedValue, negativeIdeal);
            }

            // Stockage des distances
            distancePositive[i] = distanceToPositive / 3;
            distanceNegative[i] = distanceToNegative / 3;
        }
    }
    
    
    
    
    private void displayDistanceResults() {
        // Construction d'un message pour afficher les résultats des distances
        StringBuilder sb = new StringBuilder("Distances to ideal solutions (A+ et A-):\n");

        for (int i = 0; i < numObjects; i++) {
            sb.append("Object O" + (i + 1) + ":\n");
            sb.append("Distance to the ideal positive solution (A+): " + distancePositive[i] + "\n");
            sb.append("Distance to the ideal negative solution(A-): " + distanceNegative[i] + "\n");
            sb.append("\n");
        }

        // Affichage des résultats dans une boîte de dialogue
        JOptionPane.showMessageDialog(this, sb.toString(), "Distance Results", JOptionPane.INFORMATION_MESSAGE);

        
    }

   

    // Méthode utilitaire pour calculer la distance floue
    private double calculateFuzzyDistance(double[] value, double[] ideal) {
        return Math.abs(value[0] - ideal[0]) + Math.abs(value[1] - ideal[1]) + Math.abs(value[2] - ideal[2]);
    }
    //*****
    
    private void calculateClosenessCoefficients() {
         closenessCoefficients = new double[numObjects];
        for (int i = 0; i < numObjects; i++) {
            closenessCoefficients[i] = distanceNegative[i] / (distancePositive[i] + distanceNegative[i]);
        }

        displayClosenessCoefficients(closenessCoefficients);
    }
    
    private void displayClosenessCoefficients(double[] closenessCoefficients) {
        StringBuilder sb = new StringBuilder("Object Proximity Scores :\n");
        for (int i = 0; i < numObjects; i++) {
            sb.append("O" + (i + 1) + ": " + closenessCoefficients[i] + "\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Proximity Scores", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    
    //********
    
    private void rankObjectsByCloseness() {
        // Créer un tableau d'indices pour effectuer le tri
        Integer[] indices = new Integer[numObjects];
        for (int i = 0; i < numObjects; i++) {
            indices[i] = i;
        }

        // Trier les indices en fonction des coefficients de proximité
        Arrays.sort(indices, (i1, i2) -> Double.compare(closenessCoefficients[i2], closenessCoefficients[i1]));
        
        // Affichage du classement des objets
        StringBuilder sb = new StringBuilder("Object Ranking (Best to Worst):\n");
        for (int i = 0; i < numObjects; i++) {
            sb.append("Rank " + (i + 1) + ": Object O" + (indices[i] + 1) + " (Score: " + closenessCoefficients[indices[i]] + ")\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Object Ranking", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    //*****
    public static void main(String[] args) {
        new fuzzyTopsis2();
    }
}