steps:
  - name: Checkout code
    uses: actions/checkout@v3

  - name: Set up Java
    uses: actions/setup-java@v3
    with:
      java-version: '21'

  - name: Grant executable permissions to gradlew
    run: chmod +x ./gradlew

  - name: Run Gradle tasks
    run: ./gradlew build
