package gosuslugi.database

import org.jetbrains.exposed.sql.Database

fun initDatabase() {
    Database.connect(
        "jdbc:h2:file:C:/Users/ima8/database/db1;",
        //IFEXISTS=TRUE
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    createTables()
}
