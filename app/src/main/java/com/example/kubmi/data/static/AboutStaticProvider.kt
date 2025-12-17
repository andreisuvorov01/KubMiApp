package com.example.kubmi.data.static

import com.example.kubmi.domain.model.AboutPageContent
import com.example.kubmi.domain.model.CallToAction
import com.example.kubmi.domain.model.ContactPhone
import com.example.kubmi.domain.model.ContactSection
import com.example.kubmi.domain.model.CounterItem
import com.example.kubmi.domain.model.FieldType
import com.example.kubmi.domain.model.FormField
import com.example.kubmi.domain.model.InquiryForm
import com.example.kubmi.domain.model.MapSection
import com.example.kubmi.domain.model.PersonSpotlight
import com.example.kubmi.domain.model.SocialLink

/**
 * Static snapshot of the institute's About page (elementor blocks 7647262..f318498).
 * Uses remote image URLs so the UI stays visually close to the website, but keeps
 * everything in structured native data for Android/TV rendering.
 */
object AboutStaticProvider {
    val content: AboutPageContent = AboutPageContent(
        counters = listOf(
            CounterItem(title = "Преподавателей", value = "80", suffix = "+"),
            CounterItem(title = "Кафедры", value = "8"),
            CounterItem(title = "Выпускников", value = "5000", suffix = "+")
        ),
        historyParagraphs = listOf(
            "НОЧУ ВО «Кубанский медицинский институт» – один из старейших негосударственных ВУЗов России, основанный 19 июня 1995 года на базе Кубанской государственной медицинской академии.",
            "Открытие было направлено на сокращение дефицита врачей, увеличение числа молодых специалистов в городах и сёлах края и создание современной научно-технической базы в медицине.",
            "Институт объединил талантливых студентов и опытных преподавателей – тех, кто выбрал благородную и востребованную профессию врача.",
            "За 30 лет вуз подготовил более тысячи квалифицированных врачей, работающих в России и за рубежом.",
            "Учредителями при создании стали 44 профессора Кубанской государственной медицинской академии.",
            "29 лет институт возглавлял Перов Юрий Митрофанович (1938–2024) — доктор медицинских наук, профессор, автор свыше 400 научных работ, организатор международных конгрессов «Экология и дети», двукратный лауреат премии правительства Краснодарского края."
        ),
        foundingLeaders = listOf(
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/mg_6613.jpg",
                caption = "Перов Юрий Митрофанович",
                description = "Доктор медицинских наук, профессор, основатель и первый ректор Кубанского медицинского института, автор и соавтор более 400 работ и 14 монографий, организатор международных конгрессов «Экология и дети»."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/skrinshot-04-04-2025-150039.png",
                caption = "Владимир Михайлович Покровский",
                description = "Создатель научной школы физиологии, подготовил 10 докторов и 82 кандидата наук, автор 400+ работ, член центрального совета Российского физиологического общества им. И.П. Павлова."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/80-let-prof-bensman-1024x683.jpg",
                caption = "Бенсман Владимир Михайлович",
                description = "Автор 274 научных трудов, 5 монографий, 18 авторских свидетельств и патентов. Заслуженный врач РФ, Заслуженный деятель науки Кубани, Почётный гражданин Краснодара."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/dsc03770-1024x574.jpg",
                caption = "Войцехович Борис Андреевич",
                description = "Доктор медицинских наук, профессор, академик МАНЭБ, автор около 200 научных работ и учебно-методических пособий по общественному здоровью и истории медицины."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/skrinshot-04-04-2025-150658.png",
                caption = "Фаустов Леонид Александрович",
                description = "Академик Российской Академии медико-технических наук, доктор медицинских наук, автор 200+ научных трудов, 15 монографий, 7 изобретений и 6 учебных пособий."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/skrinshot-04-04-2025-150829.png",
                caption = "Царинский Михаил Маркович",
                description = "Доктор медицинских наук, заслуженный врач России, Заслуженный деятель науки Кубани, Отличник здравоохранения, основатель научной школы терапевтической стоматологии."
            )
        ),
        todayParagraphs = listOf(
            "В институте действует 8 кафедр, 97 преподавателей, из них 8 докторов наук и 36 кандидатов наук.",
            "Защищено 3 докторских и 52 кандидатских диссертации, опубликовано 36 монографий, получено 48 патентов РФ.",
            "Институт получил 9 свидетельств на открытия в области медицины и продолжает развитие под руководством ректора Ильченко Галины Владимировны."
        ),
        todayLeaders = listOf(
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/7673c067-c64b-47d4-8862-10cd10032835-1024x768.jpg",
                caption = "Новоселя Наталья Васильевна",
                description = "Доктор медицинских наук, профессор кафедры внутренних болезней."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/81bc89be-2bee-4997-a1d7-973d4503d573-1024x768.jpg",
                caption = "Павленко Сергей Георгиевич",
                description = "Доктор медицинских наук, профессор кафедры хирургии."
            ),
            PersonSpotlight(
                imageUrl = "https://kubmi.ru/wp-content/uploads/2025/04/1a8fbd2a-cb9a-40d4-bdb6-40f88326ea91-939x1024.jpg",
                caption = "Мирзоева Рухшона Кадыровна",
                description = "Доктор биологических наук, профессор кафедры биологии."
            )
        ),
        whyChooseUs = listOf(
            "Квалифицированный преподавательский состав",
            "Современная материально-техническая база",
            "Бессрочная лицензия",
            "Практическая подготовка на базе ведущих медорганизаций Краснодара и края",
            "Традиции и инновации"
        ),
        ourAdvantages = listOf(
            "Индивидуальный подход к каждому студенту",
            "Научная деятельность",
            "Трудоустройство"
        ),
        cta = CallToAction(
            title = "Присоединяйтесь к нам!",
            body = "Кубанский медицинский институт — это путь к успешной карьере в медицине. Мы ждём абитуриентов, которые хотят стать профессионалами и помогать людям. Начните свой путь уже сегодня.",
            buttonText = "Подать документы",
            buttonUrl = "https://kubmi.ru/abitur/"
        ),
        contacts = ContactSection(
            addresses = listOf(
                "350015 г. Краснодар, ул. Буденного, 198",
                "350051 г. Краснодар, ул. Гаражная, 77"
            ),
            phones = listOf(
                ContactPhone("Зам. декана по воспитательной работе", "+7 (861) 991-42-88"),
                ContactPhone("Приемная", "+7 (861) 991-11-24"),
                ContactPhone("Учебная часть, деканат, помощник ректора", "+7 (861) 991-22-88"),
                ContactPhone("Бухгалтерия, финансовый отдел", "+7 (861) 991-30-03"),
                ContactPhone("Отдел кадров", "+7 (861) 990-44-41"),
                ContactPhone("Проректор по общим вопросам, юридический отдел", "+7 (861) 991-11-82")
            ),
            email = "info@kubmi.ru"
        ),
        socials = listOf(
            SocialLink(name = "Telegram", url = "mailto:info@kubmi.ru"),
            SocialLink(name = "VK", url = "https://vk.com"),
            SocialLink(name = "YouTube", url = "https://youtube.com")
        ),
        inquiryForm = InquiryForm(
            title = "Прием абитуриентов",
            subtitle = "Оставьте заявку и мы свяжемся с вами",
            submitText = "Отправить",
            fields = listOf(
                FormField(name = "name", placeholder = "Имя", type = FieldType.TEXT),
                FormField(name = "phone", placeholder = "Телефон", type = FieldType.PHONE),
                FormField(name = "email", placeholder = "Электронная почта", type = FieldType.EMAIL)
            )
        ),
        map = MapSection(
            title = "Кубанский медицинский институт",
            mapUrl = "https://yandex.ru/map-widget/v1/org/kubanskiy_meditsinskiy_institut/1107681058/?ll=38.981997%2C45.034356&z=17",
            externalUrl = "https://yandex.ru/maps/org/kubanskiy_meditsinskiy_institut/1107681058/?ll=38.981997%2C45.034356&z=17"
        )
    )
}

