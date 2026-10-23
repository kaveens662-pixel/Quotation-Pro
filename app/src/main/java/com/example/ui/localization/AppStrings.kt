package com.example.ui.localization

import com.example.data.preferences.AppLanguage

object AppStrings {
    fun get(key: String, lang: AppLanguage = AppLanguage.ENGLISH): String {
        val entry = STRINGS[key] ?: return key
        return if (lang == AppLanguage.SINHALA) entry.si else entry.en
    }

    private data class Loc(val en: String, val si: String)

    private val STRINGS = mapOf(
        "app_title" to Loc("Quotation Pro", "ක්වෝටේෂන් ප්‍රෝ"),
        // Bottom Nav
        "nav_dashboard" to Loc("Dashboard", "මුල් පිටුව"),
        "nav_quotations" to Loc("Quotations", "මිල ගණන්"),
        "nav_clients" to Loc("Clients", "ගනුදෙනුකරුවන්"),
        "nav_catalog" to Loc("Catalog", "භාණ්ඩ නාමාවලිය"),
        "nav_tools" to Loc("Tools", "මෙවලම්"),

        // Dashboard
        "dash_total_quoted" to Loc("Total Quoted", "මුළු මිල ගණන් වටිනාකම"),
        "dash_accepted_revenue" to Loc("Accepted Revenue", "පිළිගත් ආදායම"),
        "dash_win_rate" to Loc("Acceptance Rate", "පිළිගැනීමේ අනුපාතය"),
        "dash_quick_actions" to Loc("Quick Actions", "ක්ෂණික ක්‍රියා"),
        "dash_new_quotation" to Loc("New Quotation", "නව මිල ගණනක්"),
        "dash_new_client" to Loc("Add Client", "ගනුදෙනුකරුවෙකු එක් කරන්න"),
        "dash_new_item" to Loc("Add Product", "භාණ්ඩයක් එක් කරන්න"),
        "dash_pricing_calc" to Loc("Pricing Calculator", "මිල ගණනය"),
        "dash_recent_quotations" to Loc("Recent Quotations", "මෑත මිල ගණන් කැඳවීම්"),
        "dash_upcoming_expiries" to Loc("Expiring Soon", "ඉක්මනින් කල් ඉකුත්වන"),
        "dash_no_quotations" to Loc("No quotations created yet.", "තවම මිල ගණන් සාදා නැත."),
        "dash_no_quotations_sub" to Loc("Tap '+ New Quotation' to craft your first proposal.", "ඔබගේ පළමු යෝජනාව සකස් කිරීමට '+ නව මිල ගණනක්' තට්ටු කරන්න."),

        // Quotations
        "quote_search_hint" to Loc("Search quotation or client...", "මිල ගණනක් හෝ ගනුදෙනුකරුවෙකු සොයන්න..."),
        "quote_filter_all" to Loc("All", "සියල්ල"),
        "quote_create_title" to Loc("Create Quotation", "නව මිල ගණනක් සාදන්න"),
        "quote_edit_title" to Loc("Edit Quotation", "මිල ගණන සංස්කරණය කරන්න"),
        "quote_details_title" to Loc("Quotation Details", "මිල ගණන් විස්තර"),
        "quote_step_client" to Loc("1. Client", "1. ගනුදෙනුකරු"),
        "quote_step_items" to Loc("2. Items", "2. අයිතම"),
        "quote_step_terms" to Loc("3. Terms", "3. කොන්දේසි"),
        "quote_step_review" to Loc("4. Review", "4. සමාලෝචනය"),
        "quote_share_pdf" to Loc("Share PDF", "PDF බෙදාගන්න"),
        "quote_save_pdf" to Loc("Save PDF", "PDF සුරකින්න"),
        "quote_duplicate" to Loc("Duplicate to Draft", "පිටපත් කර කෙටුම්පත් කරන්න"),
        "quote_delete" to Loc("Delete Quotation", "මිල ගණන මකන්න"),
        "quote_change_status" to Loc("Update Status", "තත්ත්වය යාවත්කාලීන කරන්න"),

        // Clients
        "client_search_hint" to Loc("Search clients by name, company, email...", "නම, ආයතනය, විද්‍යුත් තැපෑල අනුව සොයන්න..."),
        "client_add_title" to Loc("Add Client", "ගනුදෙනුකරුවෙකු එක් කරන්න"),
        "client_edit_title" to Loc("Edit Client", "ගනුදෙනුකරු සංස්කරණය කරන්න"),
        "client_details_title" to Loc("Client Details", "ගනුදෙනුකරු විස්තර"),
        "client_name" to Loc("Client Name", "ගනුදෙනුකරුගේ නම"),
        "client_company" to Loc("Company / Organization", "ආයතනය / සමාගම"),
        "client_contact" to Loc("Contact Person", "සම්බන්ධතා පුද්ගලයා"),
        "client_phone" to Loc("Phone Number", "දුරකථන අංකය"),
        "client_email" to Loc("Email Address", "විද්‍යුත් තැපෑල"),
        "client_address" to Loc("Billing Address", "බිල්පත් ලිපිනය"),
        "client_tax_number" to Loc("Tax / Reg Number", "බදු / ලියාපදිංචි අංකය"),
        "client_notes" to Loc("Notes", "සටහන්"),

        // Catalog
        "catalog_search_hint" to Loc("Search products & services...", "භාණ්ඩ සහ සේවා සොයන්න..."),
        "catalog_add_title" to Loc("Add Catalog Item", "නව භාණ්ඩයක්/සේවාවක් එක් කරන්න"),
        "catalog_edit_title" to Loc("Edit Catalog Item", "අයිතමය සංස්කරණය කරන්න"),
        "catalog_item_name" to Loc("Item Name", "අයිතමයේ නම"),
        "catalog_item_sku" to Loc("SKU / Reference", "SKU / යොමු කේතය"),
        "catalog_item_category" to Loc("Category", "කාණ්ඩය"),
        "catalog_item_unit" to Loc("Unit Type", "ඒකක වර්ගය"),
        "catalog_item_price" to Loc("Default Unit Price", "සාමාන්‍ය ඒකක මිල"),
        "catalog_item_cost" to Loc("Cost Price (Internal)", "පිරිවැය මිල (අභ්‍යන්තර)"),
        "catalog_item_tax" to Loc("Tax Rate (%)", "බදු අනුපාතය (%)"),

        // Recurring Costs
        "rec_title" to Loc("Recurring Costs", "පුනරාවර්තන වියදම්"),
        "rec_monthly_total" to Loc("Monthly Equivalent", "මාසික සමාන වියදම"),
        "rec_annual_total" to Loc("Annual Projected", "වාර්ෂික ඇස්තමේන්තුව"),
        "rec_add_title" to Loc("Add Recurring Cost", "නව පුනරාවර්තන වියදමක්"),
        "rec_edit_title" to Loc("Edit Recurring Cost", "වියදම සංස්කරණය කරන්න"),
        "rec_frequency" to Loc("Billing Frequency", "බිල්පත් සංඛ්‍යාතය"),
        "rec_active" to Loc("Active", "ක්‍රියාකාරී"),

        // Pricing Calculator
        "calc_title" to Loc("Pricing Calculator", "මිල ගණනය කිරීමේ මෙවලම"),
        "calc_subtitle" to Loc("Calculate profitable rates with contingency & overheads", "අමතර වියදම් සහ ලාභ ආන්තිකය සමග නිවැරදි මිල ගණනය කරන්න"),
        "calc_mode_margin" to Loc("Profit Margin %", "ලාභ ආන්තිකය %"),
        "calc_mode_markup" to Loc("Cost Markup %", "පිරිවැය වැඩි කිරීම %"),
        "calc_materials" to Loc("Materials Cost", "ද්‍රව්‍ය පිරිවැය"),
        "calc_labor_hours" to Loc("Labor Hours", "ශ්‍රම පැය ගණන"),
        "calc_hourly_rate" to Loc("Hourly Rate", "පැයකට ගාස්තුව"),
        "calc_outsourcing" to Loc("Outsourcing Cost", "බාහිර සේවා පිරිවැය"),
        "calc_delivery" to Loc("Delivery / Transport", "ප්‍රවාහන ගාස්තු"),
        "calc_software_overhead" to Loc("Software & Tools Overhead", "මෘදුකාංග සහ මෙවලම් වියදම"),
        "calc_other_overhead" to Loc("Other Overhead", "වෙනත් පොදු වියදම්"),
        "calc_contingency" to Loc("Contingency (%)", "අනපේක්ෂිත වියදම් (%)"),
        "calc_target_percent" to Loc("Target Profit %", "ඉලක්කගත ලාභය %"),
        "calc_tax_percent" to Loc("Applicable Tax (%)", "අදාළ බදු (%)"),
        "calc_suggested_price" to Loc("Suggested Selling Price", "යෝජිත විකුණුම් මිල"),
        "calc_customer_price" to Loc("Final Customer Price", "අවසාන පාරිභෝගික මිල"),
        "calc_gross_profit" to Loc("Expected Gross Profit", "අපේක්ෂිත දළ ලාභය"),
        "calc_transfer_to_quote" to Loc("Use in Quotation", "මිල ගණනකට ඇතුළත් කරන්න"),

        // Business Profile
        "profile_title" to Loc("Business Profile", "ව්‍යාපාර පැතිකඩ"),
        "profile_biz_name" to Loc("Business Name", "ව්‍යාපාරයේ නම"),
        "profile_owner" to Loc("Owner / Signatory Name", "හිමිකරු / අත්සන්කරුගේ නම"),
        "profile_currency" to Loc("Default Currency", "පෙරනිමි මුදල් ඒකකය"),
        "profile_tax_rate" to Loc("Default Tax Rate (%)", "පෙරනිමි බදු අනුපාතය (%)"),
        "profile_validity" to Loc("Default Validity (Days)", "පෙරනිමි වලංගු කාලය (දින)"),
        "profile_prefix" to Loc("Quotation Prefix", "මිල ගණන් පෙරයෙදුම"),
        "profile_payment_terms" to Loc("Default Payment Terms", "පෙරනිමි ගෙවීම් කොන්දේසි"),
        "profile_footer" to Loc("Quotation Footer Text", "පාදික සටහන් පෙළ"),

        // Settings
        "settings_title" to Loc("Settings & Data", "සැකසුම් සහ දත්ත කළමනාකරණය"),
        "settings_theme" to Loc("Appearance Theme", "තේමාව"),
        "settings_language" to Loc("Language", "භාෂාව (Language)"),
        "settings_backup" to Loc("Export Backup (JSON)", "දත්ත උපස්ථයක් බාගන්න"),
        "settings_restore" to Loc("Restore Backup", "උපස්ථයකින් නැවත පිහිටුවන්න"),
        "settings_demo_data" to Loc("Load Realistic Demo Data", "ආදර්ශ දත්ත පූරණය කරන්න"),
        "settings_demo_confirm" to Loc("This will add professional sample clients, quotations, catalog items, and SaaS expenses for demonstration.", "මෙමඟින් නිරූපණය සඳහා සාම්පල සේවාදායකයින්, මිල කැඳවීම්, නාමාවලි අයිතම සහ වියදම් එක් කරනු ඇත."),
        "settings_about" to Loc("About Quotation Pro", "ක්වෝටේෂන් ප්‍රෝ ගැන"),
        "settings_privacy" to Loc("Local-First Privacy", "පෞද්ගලිකත්වය සහ දේශීය ගබඩාව"),
        "settings_privacy_desc" to Loc("All your clients, quotations, financials, and company data are securely stored 100% offline on your device.", "ඔබගේ සියලු දත්ත සහ මිල ගණන් ඔබගේ උපාංගයේ 100% ක් සුරක්ෂිතව දේශීයව පමණක් ගබඩා කර ඇත."),

        // Common Buttons
        "btn_save" to Loc("Save", "සුරකින්න"),
        "btn_cancel" to Loc("Cancel", "අවලංගු කරන්න"),
        "btn_delete" to Loc("Delete", "මකන්න"),
        "btn_confirm" to Loc("Confirm", "තහවුරු කරන්න"),
        "btn_edit" to Loc("Edit", "සංස්කරණය කරන්න"),
        "btn_done" to Loc("Done", "අවසන්"),
        "btn_next" to Loc("Next", "ඉදිරියට"),
        "btn_back" to Loc("Back", "ආපසු"),
        "btn_close" to Loc("Close", "වසන්න")
    )
}
