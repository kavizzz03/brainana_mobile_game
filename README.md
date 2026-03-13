# 🧠 BRAINANA: Neural Logic Interface
Brainana is a high-speed, neuro reactive puzzle game built with Kotlin and Jetpack Compose. It challenges players to solve complex mathematical logic puzzles in real-time, featuring a dynamic theme engine that adapts the entire UI based on your selected "Neural Vibe."

🚀 Key Features
🎨 Adaptive Theme Engine
Switch between three distinct visual and gameplay modes that change the puzzle types via dynamic API endpoints:

Neural Sync (Standard): Sleek, cyberpunk obsidian and cyan.

Super Heroes (Tomato API): High-energy red and blue theme for the bold.

Princess Protocol (Smile API): Aesthetic pink and violet hues with charming logic puzzles.

📈 Leveling & XP System
Level Up: Gain 500 XP to advance to the next level.

Neural Ranks: Progress from Initiate to Operative, and finally Architect.

Real-time Popups: Get notified instantly when you cross a level threshold during active gameplay.

🛡️ Secure Integration
Google Sign-In: Sync your progress, XP, and high scores across devices using Firebase Authentication.

Cloud Leaderboards: Compete with agents globally for the top spot on the Hall of Fame.

Persona Customization: Choose your digital identity using the integrated DiceBear Avatar API.

🎮 Gameplay Mechanics
The objective is simple: Solve the logic before the timer hits zero.

Select Difficulty: * Easy: 20s (1x Multiplier)

Medium: 12s (2x Multiplier)

Hard: 7s (4x Multiplier)

Decode: Analyze the visual pattern (Bananas, Heroes, or Smiles).

Input: Use the tactical glass keypad to enter the solution.

Sync: Gain XP for correct answers; lose XP for timeouts.

🛠️ Technical Stack
UI: 100% Jetpack Compose with custom Mesh Gradients.

Network: OkHttp for high-performance API calls.

Image Loading: Coil (Compose Image Loader) for asynchronous puzzle rendering.

Database: Firebase Firestore for real-time leaderboard syncing.

Architecture: MVVM (Model-View-ViewModel) for clean state management.

🔧 Installation & Setup
Clone the Repository:

Bash
git clone [https://github.com/yourusername/brainana.git](https://github.com/kavizzz03/brainana_mobile_game.git)

Create a project in the Firebase Console.

Add your google-services.json to the app/ directory.

Enable Anonymous and Google Authentication.

Build:

Open the project in Android Studio Hedgehog (or newer).

Sync Gradle and run on a physical device or emulator (API 24+).

📜 Credits
Logic Engine: Puzzles powered by the Marc Conrad Logic API.

Avatars: Generated via DiceBear.

Developer: Kavindu Bogahawatte/Vexel IT.
