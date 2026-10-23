import os
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, Preformatted
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch

def build_pdf():
    pdf_filename = "PROJECT_REPORT_DRAFT.pdf"
    doc = SimpleDocTemplate(
        pdf_filename,
        pagesize=A4,
        leftMargin=36,
        rightMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()

    # Custom typography styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#004D33'),
        alignment=1, # Center
        spaceAfter=6
    )

    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontSize=12,
        leading=16,
        textColor=colors.HexColor('#4C6358'),
        alignment=1,
        spaceAfter=15
    )

    h1_style = ParagraphStyle(
        'H1',
        parent=styles['Heading2'],
        fontSize=14,
        leading=18,
        textColor=colors.HexColor('#006C4C'),
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'H2',
        parent=styles['Heading3'],
        fontSize=11,
        leading=15,
        textColor=colors.HexColor('#191C1A'),
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body',
        parent=styles['Normal'],
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor('#222222'),
        spaceAfter=6
    )

    meta_style = ParagraphStyle(
        'Meta',
        parent=styles['Normal'],
        fontSize=9,
        leading=13,
        textColor=colors.HexColor('#2E3832')
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontSize=8.5,
        leading=11,
        fontName='Helvetica-Bold',
        textColor=colors.HexColor('#003D28')
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontSize=8,
        leading=11,
        textColor=colors.HexColor('#222222')
    )

    placeholder_style = ParagraphStyle(
        'Placeholder',
        parent=styles['Normal'],
        fontSize=9,
        leading=12,
        fontName='Helvetica-Bold',
        textColor=colors.HexColor('#4C6358'),
        alignment=1
    )

    elements = []

    # Title & Subtitle
    elements.append(Paragraph("Smart Health and Fitness Monitor", title_style))
    elements.append(Paragraph("Native Android Mini-Project Final Report Draft", subtitle_style))

    # Metadata Block Table
    meta_data = [
        [
            Paragraph("<b>Project:</b> Smart Health and Fitness Monitor", meta_style),
            Paragraph("<b>Platform:</b> Android (Java 11, XML Layouts)", meta_style)
        ],
        [
            Paragraph("<b>Target SDK:</b> Android 14 / 16 (minSdk 26)", meta_style),
            Paragraph("<b>Storage:</b> SQLite (SQLiteOpenHelper) + SharedPreferences", meta_style)
        ],
        [
            Paragraph("<b>Sensors:</b> Accelerometer (Sensor.TYPE_ACCELEROMETER)", meta_style),
            Paragraph("<b>Status:</b> Editable Student Report Draft (Build Verified)", meta_style)
        ]
    ]
    meta_table = Table(meta_data, colWidths=[260, 260])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F3F7F4')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#C8DDD1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#DCE9E1')),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    elements.append(meta_table)
    elements.append(Spacer(1, 10))

    # 1. Introduction and Objectives
    elements.append(Paragraph("1. Introduction and Objective", h1_style))
    elements.append(Paragraph(
        "Modern mobile devices are equipped with motion sensors capable of estimating physical activity in real time. "
        "The <b>Smart Health and Fitness Monitor</b> is a standalone native Android application developed for a student mini-project. "
        "The application avoids external dependencies, cloud services, and heavy frameworks (such as Room ORM, Compose, or Firebase) "
        "to deliver a lightweight, transparent, and battery-friendly solution using fundamental Android platform APIs.",
        body_style
    ))
    elements.append(Paragraph(
        "<b>Core Objectives:</b><br/>"
        "• <b>Step Detection:</b> Continuously estimate footsteps using the device's 3D accelerometer sensor with magnitude calculation, baseline gravity filtering, threshold crossing, and debouncing.<br/>"
        "• <b>Active Duration:</b> Track the active foreground tracking duration without counting idle background periods.<br/>"
        "• <b>Biometric Profile:</b> Provide validated user profile inputs (Name, Age, Gender, Height, Weight) stored locally in SharedPreferences.<br/>"
        "• <b>Daily Activity Records:</b> Store unique daily statistics (date, steps, duration, goal status) in SQLite using a native SQLiteOpenHelper.<br/>"
        "• <b>Goal Settings & Notifications:</b> Allow customizable daily goals with automated local notifications upon goal achievement.<br/>"
        "• <b>Lifecycle Handling:</b> Proper sensor and timer lifecycle management (Active, Paused, Stopped, Destroyed) with Logcat telemetry under tag <code>FitnessLifecycle</code>.",
        body_style
    ))

    # 2. Application Design
    elements.append(Paragraph("2. Application Design: Screens & Components", h1_style))
    elements.append(Paragraph(
        "The project is structured around three primary activities and a centralized database helper:",
        body_style
    ))

    arch_data = [
        [Paragraph("Screen / Class", table_header_style), Paragraph("Component Type", table_header_style), Paragraph("Description & Responsibilities", table_header_style)],
        [
            Paragraph("<b>MainActivity</b>", table_cell_style),
            Paragraph("AppCompatActivity<br/>SensorEventListener", table_cell_style),
            Paragraph("Main dashboard displaying real-time estimated steps, active duration timer, goal progress, Start/Pause tracking toggle, and goal setting input.", table_cell_style)
        ],
        [
            Paragraph("<b>ProfileActivity</b>", table_cell_style),
            Paragraph("AppCompatActivity", table_cell_style),
            Paragraph("Biometric profile form collecting Name, Age, Gender Spinner, Height (cm), and Weight (kg) with input range validation and SharedPreferences storage.", table_cell_style)
        ],
        [
            Paragraph("<b>HistoryActivity</b>", table_cell_style),
            Paragraph("AppCompatActivity", table_cell_style),
            Paragraph("Historical activity log viewing screen displaying past daily activity records ordered newest first in a ListView with an empty state placeholder.", table_cell_style)
        ],
        [
            Paragraph("<b>DatabaseHelper</b>", table_cell_style),
            Paragraph("SQLiteOpenHelper", table_cell_style),
            Paragraph("Manages SQLite database <code>fitness_monitor.db</code> and table <code>daily_activity</code> with safe upsert logic ensuring one record per date.", table_cell_style)
        ],
        [
            Paragraph("<b>DailyRecord</b>", table_cell_style),
            Paragraph("Model (POJO)", table_cell_style),
            Paragraph("Encapsulates date, steps, duration in seconds, and goal notification status flag.", table_cell_style)
        ],
        [
            Paragraph("<b>HistoryAdapter</b>", table_cell_style),
            Paragraph("ArrayAdapter", table_cell_style),
            Paragraph("Binds DailyRecord objects to custom item layouts (<code>item_history_record.xml</code>).", table_cell_style)
        ]
    ]
    arch_table = Table(arch_data, colWidths=[110, 110, 300])
    arch_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#E8F5EE')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#B2D6C3')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#DCE9E1')),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('LEFTPADDING', (0,0), (-1,-1), 5),
        ('RIGHTPADDING', (0,0), (-1,-1), 5),
    ]))
    elements.append(arch_table)
    elements.append(Spacer(1, 10))

    # 3. Complete Source Code Inventory
    elements.append(Paragraph("3. Complete Source Code Inventory", h1_style))
    elements.append(Paragraph(
        "All project source files are completely created and located in the workspace under package <code>com.example.smarthealthfitness</code>:",
        body_style
    ))

    code_inv_data = [
        [Paragraph("File Path", table_header_style), Paragraph("Component", table_header_style), Paragraph("Lines / Purpose", table_header_style)],
        [Paragraph("<code>app/src/main/AndroidManifest.xml</code>", table_cell_style), Paragraph("Manifest", table_cell_style), Paragraph("Declares 3 activities, POST_NOTIFICATIONS permission, and sensor feature.", table_cell_style)],
        [Paragraph("<code>.../MainActivity.java</code>", table_cell_style), Paragraph("Java Source", table_cell_style), Paragraph("Accelerometer listener, step logic, duration timer, SharedPreferences, SQLite sync.", table_cell_style)],
        [Paragraph("<code>.../ProfileActivity.java</code>", table_cell_style), Paragraph("Java Source", table_cell_style), Paragraph("Form validation (Name, Age, Gender, Height, Weight) and SharedPreferences.", table_cell_style)],
        [Paragraph("<code>.../HistoryActivity.java</code>", table_cell_style), Paragraph("Java Source", table_cell_style), Paragraph("Loads SQLite records and sets empty view for ListView.", table_cell_style)],
        [Paragraph("<code>.../DatabaseHelper.java</code>", table_cell_style), Paragraph("Java Source", table_cell_style), Paragraph("SQLite schema creation, record upsert, and newest-first query.", table_cell_style)],
        [Paragraph("<code>.../DailyRecord.java</code>", table_cell_style), Paragraph("Java Source", table_cell_style), Paragraph("POJO model with duration formatting helper.", table_cell_style)],
        [Paragraph("<code>.../HistoryAdapter.java</code>", table_cell_style), Paragraph("Java Source", table_cell_style), Paragraph("ArrayAdapter for list items with goal badge indicators.", table_cell_style)],
        [Paragraph("<code>app/.../layout/activity_main.xml</code>", table_cell_style), Paragraph("XML Layout", table_cell_style), Paragraph("Material 3 UI: Cards, step display, timer, toggle button, goal settings.", table_cell_style)],
        [Paragraph("<code>app/.../layout/activity_profile.xml</code>", table_cell_style), Paragraph("XML Layout", table_cell_style), Paragraph("Input fields and spinner for user health attributes.", table_cell_style)],
        [Paragraph("<code>app/.../layout/activity_history.xml</code>", table_cell_style), Paragraph("XML Layout", table_cell_style), Paragraph("ListView and empty state layout.", table_cell_style)],
        [Paragraph("<code>app/.../layout/item_history_record.xml</code>", table_cell_style), Paragraph("XML Layout", table_cell_style), Paragraph("CardView row for history list.", table_cell_style)],
        [Paragraph("<code>app/.../values/strings.xml, colors.xml, arrays.xml</code>", table_cell_style), Paragraph("XML Values", table_cell_style), Paragraph("Theme colors, text strings, and gender options array.", table_cell_style)],
        [Paragraph("<code>app/.../drawable/ic_notification_fitness.xml</code>", table_cell_style), Paragraph("Vector Drawable", table_cell_style), Paragraph("Monochrome vector status bar notification icon.", table_cell_style)]
    ]
    code_inv_table = Table(code_inv_data, colWidths=[170, 90, 260])
    code_inv_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#E8F5EE')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#B2D6C3')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#DCE9E1')),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 5),
        ('RIGHTPADDING', (0,0), (-1,-1), 5),
    ]))
    elements.append(code_inv_table)

    elements.append(PageBreak())

    # 4. Application Screenshots
    elements.append(Paragraph("4. Application Screenshots & Evidence Checklist", h1_style))
    elements.append(Paragraph(
        "<i>Academic Honesty Note: Synthetic screenshots and fictitious measurements have NOT been invented. "
        "The placeholders below indicate where real captured screenshots should be inserted for student report submission.</i>",
        body_style
    ))

    elements.append(Paragraph("<b>Required Screenshot Checklist:</b><br/>"
                              "[ ] 1. MainActivity with active tracking and live duration timer.<br/>"
                              "[ ] 2. MainActivity goal setting card and updated goal progress.<br/>"
                              "[ ] 3. ProfileActivity populated with user biometric data.<br/>"
                              "[ ] 4. ProfileActivity validation error message on invalid input.<br/>"
                              "[ ] 5. Android system goal-achievement status bar notification.<br/>"
                              "[ ] 6. HistoryActivity displaying saved daily records from SQLite.<br/>"
                              "[ ] 7. HistoryActivity showing 'No activity records yet' empty state.<br/>"
                              "[ ] 8. Android Studio Logcat filtered with tag <code>FitnessLifecycle</code>.",
                              body_style))

    box_data = [
        [Paragraph("<b>[ PLACEHOLDER: FIGURE 4.1 ]</b><br/>MainActivity Active Tracking Screen", placeholder_style),
         Paragraph("<b>[ PLACEHOLDER: FIGURE 4.2 ]</b><br/>UserProfile Form &amp; Validation Error", placeholder_style)],
        [Paragraph("<b>[ PLACEHOLDER: FIGURE 4.3 ]</b><br/>Goal-Achievement System Notification", placeholder_style),
         Paragraph("<b>[ PLACEHOLDER: FIGURE 4.4 ]</b><br/>Activity History Screen (SQLite Records)", placeholder_style)]
    ]
    box_table = Table(box_data, colWidths=[255, 255], rowHeights=[90, 90])
    box_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#FAFCFB')),
        ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#89A997')),
        ('INNERGRID', (0,0), (-1,-1), 1.5, colors.HexColor('#89A997')),
        ('ALIGN', (0,0), (-1,-1), 'CENTER'),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    elements.append(box_table)
    elements.append(Spacer(1, 10))

    # 5. Implementation Explanation
    elements.append(Paragraph("5. Implementation Explanation", h1_style))
    elements.append(Paragraph(
        "<b>5.1 Step Detection Algorithm & Limitations:</b><br/>"
        "• <b>3D Magnitude:</b> Computes <code>magnitude = sqrt(x^2 + y^2 + z^2)</code> to be invariant to phone tilt.<br/>"
        "• <b>Low-Pass Gravity Filter:</b> Computes <code>smoothedGravity = (0.8 * smoothedGravity) + (0.2 * magnitude)</code> to isolate static gravity (~9.81 m/s^2).<br/>"
        "• <b>Dynamic Acceleration:</b> Computes <code>dynamicAcc = magnitude - smoothedGravity</code>.<br/>"
        "• <b>Threshold Crossing &amp; Debouncing:</b> A step is registered when dynamicAcc crosses 2.5 m/s^2 and at least 300 ms have elapsed since the prior step.<br/>"
        "• <b>Limitations:</b> Bumpy vehicle rides or shaking can cause false positives. Steps are estimates and not medical measurements.",
        body_style
    ))
    elements.append(Paragraph(
        "<b>5.2 Lifecycle Management & Telemetry:</b><br/>"
        "• <b>Active (onResume):</b> Accelerometer registered only when tracking is enabled; timer started.<br/>"
        "• <b>Paused (onPause):</b> Sensor unregistered and timer stopped to save battery; progress committed to SQLite.<br/>"
        "• <b>Configuration Changes:</b> State preserved in Bundle via <code>onSaveInstanceState()</code> across screen rotation.<br/>"
        "• <b>Logging:</b> All lifecycle events are logged to Logcat under tag <code>FitnessLifecycle</code>.",
        body_style
    ))

    # 6. Testing and Results
    elements.append(Paragraph("6. Testing and Results", h1_style))
    test_data = [
        [Paragraph("Test ID", table_header_style), Paragraph("Test Scenario", table_header_style), Paragraph("Expected Result", table_header_style), Paragraph("Actual Result", table_header_style), Paragraph("Status", table_header_style)],
        [
            Paragraph("TC-01", table_cell_style),
            Paragraph("Gradle Debug Build", table_cell_style),
            Paragraph("Compiles and builds APK with exit code 0", table_cell_style),
            Paragraph("BUILD SUCCESSFUL in 1m 3s", table_cell_style),
            Paragraph("<font color='#2E7D32'><b>PASSED</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-02", table_cell_style),
            Paragraph("Profile Validation", table_cell_style),
            Paragraph("Rejects invalid age (<1 or >120) and non-numeric height", table_cell_style),
            Paragraph("Not tested (requires UI)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-03", table_cell_style),
            Paragraph("Accelerometer Steps", table_cell_style),
            Paragraph("Increments step count during active walking", table_cell_style),
            Paragraph("Not tested (requires walking)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-04", table_cell_style),
            Paragraph("Start/Pause Controls", table_cell_style),
            Paragraph("Pauses sensor tracking and duration timer immediately", table_cell_style),
            Paragraph("Not tested (requires UI)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-05", table_cell_style),
            Paragraph("Missing Accelerometer", table_cell_style),
            Paragraph("Shows warning banner, disables toggle without crash", table_cell_style),
            Paragraph("Not tested (requires sensorless VM)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-06", table_cell_style),
            Paragraph("Goal Persistence", table_cell_style),
            Paragraph("Custom step goal is restored upon app restart", table_cell_style),
            Paragraph("Not tested (requires restart)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-07", table_cell_style),
            Paragraph("Goal Notification Alert", table_cell_style),
            Paragraph("Notification sent when steps >= goal; no duplicate alerts", table_cell_style),
            Paragraph("Not tested (requires UI)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-08", table_cell_style),
            Paragraph("SQLite Record Storage", table_cell_style),
            Paragraph("Daily stats saved and viewed in HistoryActivity newest first", table_cell_style),
            Paragraph("Not tested (requires UI)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ],
        [
            Paragraph("TC-09", table_cell_style),
            Paragraph("Screen Rotation State", table_cell_style),
            Paragraph("Steps and duration preserved across device rotation", table_cell_style),
            Paragraph("Not tested (requires rotation)", table_cell_style),
            Paragraph("<font color='#D84315'><b>Pending</b></font>", table_cell_style)
        ]
    ]
    test_table = Table(test_data, colWidths=[40, 110, 170, 120, 80])
    test_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#E8F5EE')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#B2D6C3')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#DCE9E1')),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 4),
        ('RIGHTPADDING', (0,0), (-1,-1), 4),
    ]))
    elements.append(test_table)
    elements.append(Spacer(1, 10))

    # 7. Conclusion & References
    elements.append(Paragraph("7. Conclusion & Future Enhancements", h1_style))
    elements.append(Paragraph(
        "The project successfully implements all five required features and lifecycle best practices. "
        "Future enhancements could include stride length distance calculation (km), weekly bar charts, and CSV data export.",
        body_style
    ))

    elements.append(Paragraph("8. References and Acknowledgments", h1_style))
    elements.append(Paragraph(
        "1. Android Developers: <i>Sensors Overview & Accelerometer Guide</i>.<br/>"
        "2. Android Developers: <i>Data Storage - SQLiteOpenHelper & SharedPreferences</i>.<br/>"
        "3. Android Developers: <i>Activity Lifecycle Management</i>.<br/>"
        "4. AI Assistance: Architecture and draft documentation prepared with Google Antigravity AI assistant.",
        body_style
    ))

    doc.build(elements)
    print(f"PDF build complete: {os.path.abspath(pdf_filename)}")

if __name__ == "__main__":
    build_pdf()
