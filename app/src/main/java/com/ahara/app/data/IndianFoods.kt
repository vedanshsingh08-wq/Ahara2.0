package com.ahara.app.data

/** A common Indian dish for quick selection in the food diary. */
data class IndianFood(val en: String, val hi: String, val category: String, val ingredients: String)

object IndianFoods {
    /** Category keys; labels come from the string table as "fcat_<key>". */
    val categories = listOf(
        "BREAKFAST", "SOUTH_INDIAN", "ROTI_RICE", "DAL", "SABZI", "CURRY", "NONVEG",
        "SNACKS", "SWEETS", "DRINKS", "SIDES", "FRUITS"
    )

    val all: List<IndianFood> = listOf(
        // Breakfast
        IndianFood("Poha", "पोहा", "BREAKFAST", "flattened rice, onion, peanuts, curry leaves, turmeric"),
        IndianFood("Upma", "उपमा", "BREAKFAST", "semolina, onion, mustard seeds, curry leaves, vegetables"),
        IndianFood("Aloo paratha", "आलू पराठा", "BREAKFAST", "wheat flour, potato, ghee, spices"),
        IndianFood("Paneer paratha", "पनीर पराठा", "BREAKFAST", "wheat flour, paneer, ghee, spices"),
        IndianFood("Gobhi paratha", "गोभी पराठा", "BREAKFAST", "wheat flour, cauliflower, ghee, spices"),
        IndianFood("Methi thepla", "मेथी थेपला", "BREAKFAST", "wheat flour, fenugreek leaves, curd, spices"),
        IndianFood("Besan chilla", "बेसन चीला", "BREAKFAST", "gram flour, onion, tomato, green chilli"),
        IndianFood("Moong dal chilla", "मूंग दाल चीला", "BREAKFAST", "moong dal, ginger, green chilli"),
        IndianFood("Daliya", "दलिया", "BREAKFAST", "broken wheat, milk or vegetables"),
        IndianFood("Sabudana khichdi", "साबूदाना खिचड़ी", "BREAKFAST", "sago, peanuts, potato, cumin"),
        IndianFood("Puri bhaji", "पूरी भाजी", "BREAKFAST", "wheat flour, oil, potato, spices"),
        IndianFood("Chole bhature", "छोले भटूरे", "BREAKFAST", "chickpeas, refined flour, oil, spices"),
        IndianFood("Bread omelette", "ब्रेड ऑमलेट", "BREAKFAST", "bread, egg, onion, green chilli"),
        IndianFood("Boiled eggs", "उबले अंडे", "BREAKFAST", "egg"),
        IndianFood("Oats", "ओट्स", "BREAKFAST", "oats, milk or water"),
        IndianFood("Sprouts salad", "अंकुरित सलाद", "BREAKFAST", "sprouted moong, onion, tomato, lemon"),
        // South Indian
        IndianFood("Idli", "इडली", "SOUTH_INDIAN", "rice, urad dal"),
        IndianFood("Plain dosa", "सादा डोसा", "SOUTH_INDIAN", "rice, urad dal, oil"),
        IndianFood("Masala dosa", "मसाला डोसा", "SOUTH_INDIAN", "rice, urad dal, potato, oil, spices"),
        IndianFood("Uttapam", "उत्तपम", "SOUTH_INDIAN", "rice, urad dal, onion, tomato"),
        IndianFood("Medu vada", "मेदू वड़ा", "SOUTH_INDIAN", "urad dal, oil"),
        IndianFood("Sambar", "सांभर", "SOUTH_INDIAN", "toor dal, tamarind, vegetables, sambar masala"),
        IndianFood("Coconut chutney", "नारियल चटनी", "SOUTH_INDIAN", "coconut, roasted chana, green chilli"),
        IndianFood("Rasam", "रसम", "SOUTH_INDIAN", "tamarind, tomato, pepper, cumin"),
        IndianFood("Curd rice", "दही चावल", "SOUTH_INDIAN", "rice, curd, mustard seeds, curry leaves"),
        IndianFood("Lemon rice", "नींबू चावल", "SOUTH_INDIAN", "rice, lemon, peanuts, turmeric"),
        IndianFood("Pongal", "पोंगल", "SOUTH_INDIAN", "rice, moong dal, ghee, pepper, cumin"),
        IndianFood("Appam", "अप्पम", "SOUTH_INDIAN", "rice, coconut"),
        // Roti & rice
        IndianFood("Roti / Chapati", "रोटी / चपाती", "ROTI_RICE", "whole wheat flour"),
        IndianFood("Phulka with ghee", "घी वाली रोटी", "ROTI_RICE", "whole wheat flour, ghee"),
        IndianFood("Plain paratha", "सादा पराठा", "ROTI_RICE", "wheat flour, ghee"),
        IndianFood("Naan", "नान", "ROTI_RICE", "refined flour, curd, butter"),
        IndianFood("Bajra roti", "बाजरे की रोटी", "ROTI_RICE", "pearl millet flour"),
        IndianFood("Makki ki roti", "मक्की की रोटी", "ROTI_RICE", "maize flour"),
        IndianFood("Jowar roti", "ज्वार की रोटी", "ROTI_RICE", "sorghum flour"),
        IndianFood("Steamed rice", "चावल", "ROTI_RICE", "rice"),
        IndianFood("Jeera rice", "जीरा चावल", "ROTI_RICE", "rice, cumin, ghee"),
        IndianFood("Veg pulao", "वेज पुलाव", "ROTI_RICE", "rice, vegetables, whole spices"),
        IndianFood("Khichdi", "खिचड़ी", "ROTI_RICE", "rice, moong dal, ghee, turmeric"),
        IndianFood("Veg biryani", "वेज बिरयानी", "ROTI_RICE", "rice, vegetables, spices, ghee"),
        IndianFood("Rajma chawal", "राजमा चावल", "ROTI_RICE", "kidney beans, rice, onion, tomato"),
        IndianFood("Kadhi chawal", "कढ़ी चावल", "ROTI_RICE", "curd, gram flour, rice"),
        // Dal
        IndianFood("Dal tadka", "दाल तड़का", "DAL", "toor dal, ghee, cumin, garlic"),
        IndianFood("Moong dal", "मूंग दाल", "DAL", "moong dal, turmeric, cumin"),
        IndianFood("Masoor dal", "मसूर दाल", "DAL", "red lentils, onion, tomato"),
        IndianFood("Chana dal", "चना दाल", "DAL", "split chickpea, spices"),
        IndianFood("Dal makhani", "दाल मखनी", "DAL", "black urad, kidney beans, butter, cream"),
        IndianFood("Rajma", "राजमा", "DAL", "kidney beans, onion, tomato, spices"),
        IndianFood("Chole", "छोले", "DAL", "chickpeas, onion, tomato, spices"),
        IndianFood("Kadhi pakoda", "कढ़ी पकोड़ा", "DAL", "curd, gram flour, fried fritters"),
        IndianFood("Sambhar dal", "सांभर दाल", "DAL", "toor dal, vegetables, tamarind"),
        // Sabzi
        IndianFood("Aloo gobhi", "आलू गोभी", "SABZI", "potato, cauliflower, spices"),
        IndianFood("Bhindi", "भिंडी", "SABZI", "okra, onion, spices"),
        IndianFood("Palak sabzi", "पालक सब्जी", "SABZI", "spinach, garlic, spices"),
        IndianFood("Lauki sabzi", "लौकी सब्जी", "SABZI", "bottle gourd, tomato, spices"),
        IndianFood("Tori sabzi", "तोरई सब्जी", "SABZI", "ridge gourd, spices"),
        IndianFood("Karela", "करेला", "SABZI", "bitter gourd, onion, spices"),
        IndianFood("Baingan bharta", "बैंगन भरता", "SABZI", "roasted brinjal, onion, tomato"),
        IndianFood("Aloo matar", "आलू मटर", "SABZI", "potato, peas, tomato, spices"),
        IndianFood("Mixed veg", "मिक्स वेज", "SABZI", "seasonal vegetables, spices"),
        IndianFood("Cabbage sabzi", "पत्तागोभी सब्जी", "SABZI", "cabbage, peas, spices"),
        IndianFood("Sarson ka saag", "सरसों का साग", "SABZI", "mustard greens, spinach, maize flour, ghee"),
        IndianFood("Jeera aloo", "जीरा आलू", "SABZI", "potato, cumin"),
        IndianFood("Soya chunks sabzi", "सोया चंक्स सब्जी", "SABZI", "soya chunks, onion, tomato, spices"),
        // Paneer & curries
        IndianFood("Palak paneer", "पालक पनीर", "CURRY", "spinach, paneer, cream, spices"),
        IndianFood("Matar paneer", "मटर पनीर", "CURRY", "peas, paneer, tomato gravy"),
        IndianFood("Paneer butter masala", "पनीर बटर मसाला", "CURRY", "paneer, butter, tomato, cream"),
        IndianFood("Shahi paneer", "शाही पनीर", "CURRY", "paneer, cashew, cream"),
        IndianFood("Kadai paneer", "कड़ाही पनीर", "CURRY", "paneer, capsicum, onion, spices"),
        IndianFood("Malai kofta", "मलाई कोफ्ता", "CURRY", "paneer-potato dumplings, cream gravy"),
        IndianFood("Dum aloo", "दम आलू", "CURRY", "baby potato, curd gravy"),
        IndianFood("Mushroom masala", "मशरूम मसाला", "CURRY", "mushroom, onion, tomato"),
        // Non-veg
        IndianFood("Chicken curry", "चिकन करी", "NONVEG", "chicken, onion, tomato, spices"),
        IndianFood("Butter chicken", "बटर चिकन", "NONVEG", "chicken, butter, tomato, cream"),
        IndianFood("Chicken biryani", "चिकन बिरयानी", "NONVEG", "chicken, rice, spices, ghee"),
        IndianFood("Tandoori chicken", "तंदूरी चिकन", "NONVEG", "chicken, curd, spices"),
        IndianFood("Mutton curry", "मटन करी", "NONVEG", "mutton, onion, spices"),
        IndianFood("Fish curry", "मछली करी", "NONVEG", "fish, tomato or coconut gravy"),
        IndianFood("Egg curry", "अंडा करी", "NONVEG", "egg, onion, tomato, spices"),
        IndianFood("Egg bhurji", "अंडा भुर्जी", "NONVEG", "egg, onion, tomato, chilli"),
        // Snacks
        IndianFood("Samosa", "समोसा", "SNACKS", "refined flour, potato, peas, oil"),
        IndianFood("Kachori", "कचौरी", "SNACKS", "refined flour, dal filling, oil"),
        IndianFood("Pakoda", "पकोड़ा", "SNACKS", "gram flour, onion or vegetables, oil"),
        IndianFood("Dhokla", "ढोकला", "SNACKS", "gram flour, curd, mustard seeds"),
        IndianFood("Pav bhaji", "पाव भाजी", "SNACKS", "mixed vegetables, butter, pav bread"),
        IndianFood("Vada pav", "वड़ा पाव", "SNACKS", "potato fritter, pav bread, chutney"),
        IndianFood("Pani puri", "पानी पूरी", "SNACKS", "puri, potato, tamarind water"),
        IndianFood("Bhel puri", "भेल पूरी", "SNACKS", "puffed rice, sev, onion, chutney"),
        IndianFood("Aloo tikki", "आलू टिक्की", "SNACKS", "potato, spices, oil"),
        IndianFood("Maggi / noodles", "मैगी / नूडल्स", "SNACKS", "instant noodles"),
        IndianFood("Roasted chana", "भुने चने", "SNACKS", "roasted chickpeas"),
        IndianFood("Makhana", "मखाना", "SNACKS", "fox nuts, ghee"),
        IndianFood("Namkeen", "नमकीन", "SNACKS", "gram flour mixture, oil"),
        IndianFood("Biscuits", "बिस्कुट", "SNACKS", "refined flour, sugar"),
        IndianFood("Peanuts", "मूंगफली", "SNACKS", "peanuts"),
        IndianFood("Dry fruits", "सूखे मेवे", "SNACKS", "almonds, cashews, raisins, walnuts"),
        // Sweets
        IndianFood("Kheer", "खीर", "SWEETS", "rice, milk, sugar, cardamom"),
        IndianFood("Halwa (sooji)", "सूजी हलवा", "SWEETS", "semolina, ghee, sugar"),
        IndianFood("Gajar halwa", "गाजर हलवा", "SWEETS", "carrot, milk, ghee, sugar"),
        IndianFood("Gulab jamun", "गुलाब जामुन", "SWEETS", "khoa, sugar syrup"),
        IndianFood("Rasgulla", "रसगुल्ला", "SWEETS", "chhena, sugar syrup"),
        IndianFood("Jalebi", "जलेबी", "SWEETS", "refined flour, sugar syrup, oil"),
        IndianFood("Laddoo", "लड्डू", "SWEETS", "gram flour or semolina, ghee, sugar"),
        IndianFood("Barfi", "बर्फी", "SWEETS", "khoa, sugar"),
        IndianFood("Jaggery (gur)", "गुड़", "SWEETS", "jaggery"),
        // Drinks
        IndianFood("Chai", "चाय", "DRINKS", "tea, milk, sugar, ginger"),
        IndianFood("Coffee", "कॉफ़ी", "DRINKS", "coffee, milk, sugar"),
        IndianFood("Milk", "दूध", "DRINKS", "milk"),
        IndianFood("Haldi doodh", "हल्दी दूध", "DRINKS", "milk, turmeric"),
        IndianFood("Lassi", "लस्सी", "DRINKS", "curd, sugar or salt"),
        IndianFood("Chaas / buttermilk", "छाछ", "DRINKS", "buttermilk, cumin, salt"),
        IndianFood("Nimbu pani", "नींबू पानी", "DRINKS", "lemon, water, sugar or salt"),
        IndianFood("Coconut water", "नारियल पानी", "DRINKS", "tender coconut water"),
        IndianFood("Fruit juice", "फलों का रस", "DRINKS", "fruit juice"),
        IndianFood("Soft drink", "कोल्ड ड्रिंक", "DRINKS", "carbonated drink, sugar"),
        // Sides
        IndianFood("Curd / dahi", "दही", "SIDES", "curd"),
        IndianFood("Raita", "रायता", "SIDES", "curd, cucumber or boondi"),
        IndianFood("Green salad", "सलाद", "SIDES", "cucumber, onion, tomato, carrot"),
        IndianFood("Papad", "पापड़", "SIDES", "urad dal papad"),
        IndianFood("Pickle (achar)", "अचार", "SIDES", "mango or lemon, mustard oil, spices"),
        IndianFood("Green chutney", "हरी चटनी", "SIDES", "coriander, mint, green chilli"),
        IndianFood("Ghee", "घी", "SIDES", "ghee"),
        // Fruits
        IndianFood("Banana", "केला", "FRUITS", "banana"),
        IndianFood("Apple", "सेब", "FRUITS", "apple"),
        IndianFood("Mango", "आम", "FRUITS", "mango"),
        IndianFood("Papaya", "पपीता", "FRUITS", "papaya"),
        IndianFood("Guava", "अमरूद", "FRUITS", "guava"),
        IndianFood("Orange", "संतरा", "FRUITS", "orange"),
        IndianFood("Pomegranate", "अनार", "FRUITS", "pomegranate"),
        IndianFood("Watermelon", "तरबूज़", "FRUITS", "watermelon"),
        IndianFood("Grapes", "अंगूर", "FRUITS", "grapes"),
        IndianFood("Chikoo", "चीकू", "FRUITS", "sapodilla")
    )

    /** Display name for the current language (Hindi script for hi and sa). */
    fun name(f: IndianFood, lang: String): String = if (lang == "en") f.en else f.hi

    /** Categories suggested first for a meal type. */
    fun suggestedCategory(mealType: String): String = when (mealType) {
        MealType.BREAKFAST.name -> "BREAKFAST"
        MealType.SNACK.name -> "SNACKS"
        else -> "ROTI_RICE"
    }
}
