import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, KeepTogether
)

def create_credentials_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()

    # Custom styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#0F172A'),
        spaceAfter=4
    )

    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#475569'),
        spaceAfter=12
    )

    section_heading = ParagraphStyle(
        'SectionHeading',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=colors.HexColor('#1E3A8A'),
        spaceBefore=14,
        spaceAfter=6
    )

    body_bold = ParagraphStyle(
        'BodyBold',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12,
        textColor=colors.HexColor('#1E293B')
    )

    body_val = ParagraphStyle(
        'BodyVal',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=9,
        leading=12,
        textColor=colors.HexColor('#0F172A')
    )

    body_desc = ParagraphStyle(
        'BodyDesc',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=12,
        textColor=colors.HexColor('#334155')
    )

    notice_style = ParagraphStyle(
        'NoticeStyle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12,
        textColor=colors.HexColor('#991B1B')
    )

    story = []

    # Title & Header Banner
    story.append(Paragraph("SMC Management Enterprise", title_style))
    story.append(Paragraph("Official System Credentials & Production Configuration Vault", subtitle_style))
    
    # Confidentiality Alert Box
    alert_data = [
        [
            Paragraph("CONFIDENTIALITY & SECURITY NOTICE", notice_style),
        ],
        [
            Paragraph(
                "This document contains high-security credentials, database passwords, encryption secrets, "
                "and production server configurations for BusinessManager Enterprise. Keep this file stored in a "
                "secure, encrypted location.",
                body_desc
            )
        ]
    ]
    alert_table = Table(alert_data, colWidths=[540])
    alert_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#FEF2F2')),
        ('BOX', (0, 0), (-1, -1), 1, colors.HexColor('#FCA5A5')),
        ('PADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 2),
    ]))
    story.append(alert_table)
    story.append(Spacer(1, 10))

    def make_table(data, col_widths=[180, 200, 160]):
        formatted_data = []
        for row in data:
            formatted_row = [
                Paragraph(row[0], body_bold),
                Paragraph(row[1], body_val),
                Paragraph(row[2] if len(row) > 2 else "", body_desc)
            ]
            formatted_data.append(formatted_row)
        
        t = Table(formatted_data, colWidths=col_widths)
        t.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#F8FAFC')),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#E2E8F0')),
            ('PADDING', (0, 0), (-1, -1), 5),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ]))
        return t

    # 1. System Administration User Account
    story.append(Paragraph("1. Super Administrator Account", section_heading))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#1E3A8A'), spaceAfter=6))
    admin_data = [
        ["Initial Admin Username", "sethu", "Primary System Administrator account"],
        ["Default Admin Password", "Sethupathi#*$1", "Default master password"],
        ["Granted Role", "ROLE_ADMINISTRATOR", "Full system access & administration"],
    ]
    story.append(make_table(admin_data))
    story.append(Spacer(1, 10))

    # 2. Database Credentials
    story.append(Paragraph("2. MySQL Production Database Credentials", section_heading))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#1E3A8A'), spaceAfter=6))
    db_data = [
        ["Database Host / Service", "mysql (port 3306)", "Docker internal MySQL network host"],
        ["Database Name", "businessmanager_prod", "Production schema name"],
        ["Database Username", "businessmanager", "Application DB user account"],
        ["Database Password", "StrongDBPassword123!", "Application DB password"],
        ["MySQL Root Password", "StrongRootPassword123!", "MySQL root administrative password"],
    ]
    story.append(make_table(db_data))
    story.append(Spacer(1, 10))

    # 3. Security, Encryption & Tokens
    story.append(Paragraph("3. Authentication, Cryptography & Token Keys", section_heading))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#1E3A8A'), spaceAfter=6))
    sec_data = [
        ["JWT Secret Key", "BusinessManager_JWT_2026_StrongSecret_9xK7mP2q", "HMAC-SHA token signing key"],
        ["JWT Access Expiration", "3600000 ms (1 Hour)", "Duration of access token valid period"],
        ["JWT Refresh Expiration", "604800000 ms (7 Days)", "Duration of refresh token valid period"],
        ["Crypto Field Secret", "BusinessManagerCrypto_2026_X7k9P2m4", "AES encryption key for sensitive DB data"],
    ]
    story.append(make_table(sec_data))
    story.append(Spacer(1, 10))

    # 4. Automated Backup & Recovery Config
    story.append(Paragraph("4. Automated Backup & Recovery System", section_heading))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#1E3A8A'), spaceAfter=6))
    backup_data = [
        ["Backup Engine Enabled", "true", "Automated cron scheduled backups active"],
        ["Storage Directory", "/opt/businessmanager/backups", "Mounted host volume directory"],
        ["Backup Cron Schedule", "0 0 2 * * ?", "Triggers daily at 02:00 AM"],
        ["Retention Enabled", "true", "Automatic purge of legacy backup files"],
        ["Retention Period", "30 Days", "Keeps backups for 30 calendar days"],
        ["Retention Cron Schedule", "0 0 3 * * ?", "Purge cleanup runs daily at 03:00 AM"],
        ["Backup Encryption Key", "BusinessManagerBackup_2026_X7k9P2m4", "Key used to encrypt dump archives"],
        ["Database CLI Tools", "mysqldump / mysql", "Internal utilities for dump/restore"],
    ]
    story.append(make_table(backup_data))
    story.append(Spacer(1, 10))

    # 5. Network & Server Environment
    story.append(Paragraph("5. Server Network & Docker Deployment", section_heading))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#1E3A8A'), spaceAfter=6))
    net_data = [
        ["Spring Profile Active", "prod", "Production runtime profile"],
        ["Container Server Port", "8080", "Backend HTTP server listener port"],
        ["CORS Allowed Origins", "http://192.168.1.8:8080", "Authorized cross-origin web client IP"],
        ["Docker Compose Specification", "docker-compose.prod.yml", "Production containers definition file"],
        ["Environment Vault File", ".env", "Server-level environment variables file"],
    ]
    story.append(make_table(net_data))
    story.append(Spacer(1, 14))

    # Footer summary note
    footer_p = Paragraph(
        "<b>Document generated on:</b> 2026-08-30 | <b>System:</b> SMC Management Enterprise (Senthur Chemical)<br/>"
        "<i>Store this document safely. Do not share credentials over unencrypted channels.</i>",
        body_desc
    )
    story.append(footer_p)

    doc.build(story)
    print(f"PDF generated successfully at {filename}")

if __name__ == '__main__':
    target_path = r"d:\DEVELOPMENT\BusinessManagerEnterprise\Production_Credentials_Config.pdf"
    create_credentials_pdf(target_path)
