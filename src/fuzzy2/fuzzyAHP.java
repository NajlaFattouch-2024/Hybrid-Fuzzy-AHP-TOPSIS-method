package fuzzyAHP;

import javax.swing.*;
import java.awt.*;

public class fuzzyAHP extends JFrame {
    private final int numCriteria = 3; // Définir 7 critères
    private final double[][][] fuzzyMatrix;

    public fuzzyAHP() {
        this.fuzzyMatrix = new double[numCriteria][numCriteria][3];
        
        setTitle("Fuzzy AHP - Pairwise Comparison");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600); // Taille ajustée pour 7 critères
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Gives fuzzy value for each criterion :", JLabel.CENTER), BorderLayout.NORTH);

        JPanel matrixPanel = new JPanel(new GridLayout(numCriteria + 1, numCriteria + 1));
        JTextField[][][] fields = new JTextField[numCriteria][numCriteria][3];

        for (int i = 0; i <= numCriteria; i++) {
            for (int j = 0; j <= numCriteria; j++) {
                if (i == 0 && j == 0) {
                    matrixPanel.add(new JLabel("")); // Case vide en haut à gauche
                } else if (i == 0) {
                    matrixPanel.add(new JLabel("C" + j, JLabel.CENTER)); // Labels des colonnes
                } else if (j == 0) {
                    matrixPanel.add(new JLabel("C" + i, JLabel.CENTER)); // Labels des lignes
                } else if (i == j) {
                    JTextField field = new JTextField("(1, 1, 1)");
                    field.setEditable(false); // Valeurs fixes pour la diagonale
                    fuzzyMatrix[i - 1][j - 1] = new double[]{1, 1, 1};
                    matrixPanel.add(field);
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
        JButton validateButton = new JButton("Validate & Terminate");
        validateButton.addActionListener(e -> {
            if (saveFuzzyMatrix(fields)) {
                calculateGeometricFuzzyWeights(); // Calculer les poids flous géométriques
                JOptionPane.showMessageDialog(this, 
                        "Saved value. Pass to the next step.",
                        "Successed", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0); // Facultatif : fermer l'application après validation
            }
        });
        buttonPanel.add(validateButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        add(panel);
    }

    private boolean saveFuzzyMatrix(JTextField[][][] fields) {
        for (int i = 0; i < numCriteria; i++) {
            for (int j = 0; j < numCriteria; j++) {
                if (i != j) {
                    try {
                        for (int k = 0; k < 3; k++) {
                            String value = fields[i][j][k].getText().trim(); // Supprimez les espaces superflus
                            if (value.isEmpty()) {
                                throw new NumberFormatException("Empty value.");
                            }
                            fuzzyMatrix[i][j][k] = Double.parseDouble(value);
                        }
                        if (!isTriangularValid(fuzzyMatrix[i][j])) {
                            throw new IllegalArgumentException("values are not fuzzy.");
                        }
                        fuzzyMatrix[j][i] = getReciprocal(fuzzyMatrix[i][j]);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, 
                                "Error on the criterion field C" + (i + 1) + " and C" + (j + 1) + ": " + ex.getMessage(), 
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(this, 
                                "Error on the criterion field C" + (i + 1) + " and C" + (j + 1) + ": " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isTriangularValid(double[] fuzzyNumber) {
        return fuzzyNumber[0] <= fuzzyNumber[1] && fuzzyNumber[1] <= fuzzyNumber[2];
    }

    private double[] getReciprocal(double[] fuzzyNumber) {
        return new double[] {
            1.0 / fuzzyNumber[2],
            1.0 / fuzzyNumber[1],
            1.0 / fuzzyNumber[0]
        };
    }

    private void calculateGeometricFuzzyWeights() {
        double[] geometricMeansMin = new double[numCriteria];
        double[] geometricMeansMed = new double[numCriteria];
        double[] geometricMeansMax = new double[numCriteria];

        for (int i = 0; i < numCriteria; i++) {
            double productMin = 1.0;
            double productMed = 1.0;
            double productMax = 1.0;

            for (int j = 0; j < numCriteria; j++) {
                if (i != j) {
                    productMin *= fuzzyMatrix[i][j][0]; 
                    productMed *= fuzzyMatrix[i][j][1]; 
                    productMax *= fuzzyMatrix[i][j][2]; 
                }
            }

            geometricMeansMin[i] = productMin;
            geometricMeansMed[i] = productMed;
            geometricMeansMax[i] = productMax;
        }

        double[] normalizedMin = new double[numCriteria];
        double[] normalizedMed = new double[numCriteria];
        double[] normalizedMax = new double[numCriteria];

        for (int i = 0; i < numCriteria; i++) {
            normalizedMin[i] = Math.cbrt(geometricMeansMin[i]);
            normalizedMed[i] = Math.cbrt(geometricMeansMed[i]);
            normalizedMax[i] = Math.cbrt(geometricMeansMax[i]);
        }

        double sumMin = 0.0, sumMed = 0.0, sumMax = 0.0;
        for (int i = 0; i < numCriteria; i++) {
            sumMin += normalizedMin[i];
            sumMed += normalizedMed[i];
            sumMax += normalizedMax[i];
        }

        double[] finalNormalizedMin = new double[numCriteria];
        double[] finalNormalizedMed = new double[numCriteria];
        double[] finalNormalizedMax = new double[numCriteria];

        for (int i = 0; i < numCriteria; i++) {
            finalNormalizedMin[i] = normalizedMin[i] / sumMin;
            finalNormalizedMed[i] = normalizedMed[i] / sumMed;
            finalNormalizedMax[i] = normalizedMax[i] / sumMax;
        }

        StringBuilder result = new StringBuilder("Normalized fuzzy weigths :\n");
        for (int i = 0; i < numCriteria; i++) {
            result.append("C").append(i + 1).append(": (")
                  .append(finalNormalizedMin[i]).append(", ")
                  .append(finalNormalizedMed[i]).append(", ")
                  .append(finalNormalizedMax[i]).append(")\n");
        }

        JOptionPane.showMessageDialog(this, result.toString(),
                "Normalized fuzzy weigths", JOptionPane.INFORMATION_MESSAGE);

        defuzzifyAndDisplayWeights(finalNormalizedMin, finalNormalizedMed, finalNormalizedMax);
    }

    private void defuzzifyAndDisplayWeights(double[] finalNormalizedMin, double[] finalNormalizedMed, double[] finalNormalizedMax) {
        double[] defuzzifiedWeights = new double[numCriteria];

        for (int i = 0; i < numCriteria; i++) {
            defuzzifiedWeights[i] = (finalNormalizedMin[i] + finalNormalizedMed[i] + finalNormalizedMax[i]) / 3;
        }

        StringBuilder result = new StringBuilder("Non fuzzy weigths :\n");
        for (int i = 0; i < numCriteria; i++) {
            result.append("C").append(i + 1).append(": ").append(defuzzifiedWeights[i]).append("\n");
        }

        JOptionPane.showMessageDialog(this, result.toString(),
                "Non fuzzy weigths", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(fuzzyAHP::new);
    }
}








