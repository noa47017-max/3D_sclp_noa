public class PheromoneNode {
    double concentration = 1.0; // ריכוז הפרומונים ההתחלתי [9]

    // נתוני טעינה אופטימליים שנמצאו למצב זה (משוואה 10) [1]
    int bestDirection;
    double xB, yB, zB; // פינה תחתונה
    double xT, yT, zT; // פינה עליונה
    double residualSpace; // VRi
}
