package com.nexus.personalization;

public class TeammateProfile {

    public enum Gender { MALE, FEMALE }

    private final String id;
    private final String name;
    private final String rollNo;
    private final String studentId;
    private final Gender gender;
    private final String role;
    private final String preferredTone;
    private final String interestClusters;
    private final String welcomePhrase;

    public TeammateProfile(String id, String name, String rollNo, String studentId,
                           Gender gender, String role, String preferredTone,
                           String interestClusters, String welcomePhrase) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
        this.studentId = studentId;
        this.gender = gender;
        this.role = role;
        this.preferredTone = preferredTone;
        this.interestClusters = interestClusters;
        this.welcomePhrase = welcomePhrase;
    }

    public static TeammateProfile[] getAllTeammates() {
        return new TeammateProfile[] {
            new TeammateProfile(
                "ABHIJAY", "Abhijay L. G.", "121", "STI25CS",
                Gender.MALE, "Python AI Perception Core, Central Orchestration & Adaptive Personalization",
                "Technical, Precise, & Proactive", "Python AI, Deep Learning, Vision Transformers & Event Orchestration",
                "Welcome back Abhijay. Python AI perception engine and central orchestrator are fully operational."
            ),
            new TeammateProfile(
                "BHADRA", "Bhadra G. S.", "48", "STI25CS048",
                Gender.FEMALE, "Project Ideation, Architecture & Problem Statement",
                "Analytical, Visionary, & Structured", "System Architecture, Multi-modal AI, Research",
                "Greetings Bhadra. Project N.E.X.U.S architecture and state tracking are nominal."
            ),
            new TeammateProfile(
                "ALEENA", "Aleena Maria Roy", "26", "STI25CS026",
                Gender.FEMALE, "Computer Vision, OpenCV & ONNX Modeling",
                "Insightful, Visual, & Scientific", "OpenCV, ONNX Runtime, Emotion & Face Perception",
                "Hello Aleena. Visual perception pipeline and ONNX emotion classifiers are active."
            ),
            new TeammateProfile(
                "ABHISHEK", "Abhishek A.", "09", "STI25CS009",
                Gender.MALE, "Speech I/O Subsystem (Vosk, Piper, Porcupine)",
                "Energetic, Direct, & Audio-focused", "Vosk STT, Piper TTS, Acoustic Wake Word",
                "Welcome Abhishek. Speech I/O engine, audio visualizer, and acoustic pipelines are online."
            ),
            new TeammateProfile(
                "DIA", "Dia M. Joby", "52", "STI25CS052",
                Gender.FEMALE, "Frontend JavaFX HUD & Dynamic Visualizer",
                "Creative, Detail-oriented, & Aesthetic", "JavaFX HUD, Cyberpunk Styling, Real-Time Audio Canvas",
                "Hello Dia. Cyberpunk HUD interface, telemetry meters, and visual canvas are rendering at sixty FPS."
            )
        };
    }

    public static TeammateProfile findById(String id) {
        for (TeammateProfile t : getAllTeammates()) {
            if (t.getId().equalsIgnoreCase(id)) return t;
        }
        return getAllTeammates()[0];
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRollNo() { return rollNo; }
    public String getStudentId() { return studentId; }
    public Gender getGender() { return gender; }
    public String getRole() { return role; }
    public String getPreferredTone() { return preferredTone; }
    public String getInterestClusters() { return interestClusters; }
    public String getWelcomePhrase() { return welcomePhrase; }
}
