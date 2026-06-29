package com.example.kubmi.domain.model

data class AboutPageContent(
    val counters: List<CounterItem>,
    val historyParagraphs: List<String>,
    val foundingLeaders: List<PersonSpotlight>,
    val todayParagraphs: List<String>,
    val todayLeaders: List<PersonSpotlight>,
    val whyChooseUs: List<String>,
    val ourAdvantages: List<String>,
    val cta: CallToAction,
    val contacts: ContactSection,
    val socials: List<SocialLink>,
    val inquiryForm: InquiryForm,
    val map: MapSection
)

data class CounterItem(
    val title: String,
    val value: String,
    val suffix: String = ""
)

data class PersonSpotlight(
    val imageUrl: String,
    val caption: String,
    val description: String
)

data class CallToAction(
    val title: String,
    val body: String,
    val buttonText: String,
    val buttonUrl: String
)

data class ContactSection(
    val addresses: List<String>,
    val phones: List<ContactPhone>,
    val email: String
)

data class ContactPhone(
    val label: String,
    val number: String
)

data class SocialLink(
    val name: String,
    val url: String
)

data class InquiryForm(
    val title: String,
    val subtitle: String,
    val submitText: String,
    val fields: List<FormField>
)

data class FormField(
    val name: String,
    val placeholder: String,
    val type: FieldType
)

enum class FieldType {
    TEXT,
    PHONE,
    EMAIL
}

data class MapSection(
    val title: String,
    val mapUrl: String,
    val externalUrl: String
)

