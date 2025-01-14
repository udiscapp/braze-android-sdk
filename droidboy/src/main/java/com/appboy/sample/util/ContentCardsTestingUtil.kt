package com.appboy.sample.util

import android.content.Context
import com.braze.Braze
import com.braze.enums.CardKey
import com.braze.enums.CardType
import com.braze.models.cards.Card
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("UnsafeCallOnNullableType")
class ContentCardsTestingUtil private constructor() {
    companion object {
        /**
         * https://effigis.com/en/solutions/satellite-images/satellite-image-samples/
         */
        private val SUPER_HIGH_RESOLUTION_IMAGES = listOf(
            "https://images.unsplash.com/photo-1543314444-26a64fa5efe1"
        )
        private val random = Random()

        fun createRandomCards(context: Context, numCardsOfEachType: Int): List<Card> {
            val cards = mutableListOf<Card>()

            for (cardType in CardType.values()) {
                if (cardType == CardType.DEFAULT) {
                    continue
                }
                repeat((0..numCardsOfEachType).count()) {
                    createRandomCard(context, cardType)?.let { card -> cards.add(card) }
                }
            }

            cards.shuffle()
            return cards
        }

        fun getRemovedCardJson(id: String): JSONObject {
            val ccp = CardKey.Provider(true)
            return JSONObject(
                mapOf(
                    ccp.getKey(CardKey.ID) to id,
                    ccp.getKey(CardKey.REMOVED) to true,
                )
            )
        }

        fun createCaptionedImageCardJson(
            id: String,
            title: String,
            description: String,
            imageUrl: String
        ): JSONObject {
            val ccp = CardKey.Provider(true)

            // Get the default fields
            val defaultMapping = getDefaultCardFields(ccp, CardType.CAPTIONED_IMAGE)

            defaultMapping.mergeWith(
                mapOf(
                    ccp.getKey(CardKey.ID) to id,
                    ccp.getKey(CardKey.CAPTIONED_IMAGE_IMAGE) to imageUrl,
                    ccp.getKey(CardKey.CAPTIONED_IMAGE_ASPECT_RATIO) to 1.0,
                    ccp.getKey(CardKey.CAPTIONED_IMAGE_TITLE) to title,
                    ccp.getKey(CardKey.CAPTIONED_IMAGE_DESCRIPTION) to description,
                    ccp.getKey(CardKey.PINNED) to true,
                    ccp.getKey(CardKey.DISMISSIBLE) to false,
                    ccp.getKey(CardKey.CREATED) to System.currentTimeMillis()
                )
            )
            return JSONObject(defaultMapping.toMap())
        }

        private fun getDefaultCardFields(ccp: CardKey.Provider, cardType: CardType): MutableMap<String, Any> = mutableMapOf(
            ccp.getKey(CardKey.ID) to getRandomString(),
            ccp.getKey(CardKey.TYPE) to ccp.getServerKeyFromCardType(cardType)!!,
            ccp.getKey(CardKey.VIEWED) to getRandomBoolean(),
            ccp.getKey(CardKey.CREATED) to getNow(),
            ccp.getKey(CardKey.EXPIRES_AT) to getNowPlusDelta(TimeUnit.DAYS, 30),
            ccp.getKey(CardKey.OPEN_URI_IN_WEBVIEW) to getRandomBoolean(),
            ccp.getKey(CardKey.DISMISSED) to false,
            ccp.getKey(CardKey.REMOVED) to false,
            ccp.getKey(CardKey.PINNED) to getRandomBoolean(),
            ccp.getKey(CardKey.DISMISSIBLE) to getRandomBoolean(),
            ccp.getKey(CardKey.IS_TEST) to true
        )

        private fun createRandomCard(context: Context, cardType: CardType): Card? {
            val ccp = CardKey.Provider(true)

            // Set the default fields
            val defaultMapping = getDefaultCardFields(ccp, cardType)

            // Based on the card type, add new fields
            val title = "Title"
            val description = "Description -> cardType $cardType"
            val randomImage = getRandomImageUrl()

            when (cardType) {
                CardType.IMAGE -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            ccp.getKey(CardKey.IMAGE_ONLY_IMAGE) to randomImage.first,
                            ccp.getKey(CardKey.IMAGE_ONLY_ASPECT_RATIO) to randomImage.second,
                            ccp.getKey(CardKey.IMAGE_ONLY_URL) to randomImage.first
                        )
                    )
                }
                CardType.CAPTIONED_IMAGE -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            ccp.getKey(CardKey.CAPTIONED_IMAGE_IMAGE) to randomImage.first,
                            ccp.getKey(CardKey.CAPTIONED_IMAGE_ASPECT_RATIO) to randomImage.second,
                            ccp.getKey(CardKey.CAPTIONED_IMAGE_TITLE) to title,
                            ccp.getKey(CardKey.CAPTIONED_IMAGE_DESCRIPTION) to description,
                            ccp.getKey(CardKey.CAPTIONED_IMAGE_URL) to randomImage.first
                        )
                    )
                }
                CardType.SHORT_NEWS -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            ccp.getKey(CardKey.SHORT_NEWS_IMAGE) to randomImage.first,
                            ccp.getKey(CardKey.SHORT_NEWS_TITLE) to title,
                            ccp.getKey(CardKey.SHORT_NEWS_DESCRIPTION) to description,
                            ccp.getKey(CardKey.SHORT_NEWS_URL) to randomImage.first
                        )
                    )
                }
                CardType.TEXT_ANNOUNCEMENT -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            ccp.getKey(CardKey.TEXT_ANNOUNCEMENT_DESCRIPTION) to description,
                            ccp.getKey(CardKey.TEXT_ANNOUNCEMENT_URL) to randomImage.first,
                            ccp.getKey(CardKey.TEXT_ANNOUNCEMENT_TITLE) to title,
                            ccp.getKey(CardKey.TEXT_ANNOUNCEMENT_URL) to randomImage.first
                        )
                    )
                }
                else -> {
                    // Do nothing!
                }
            }

            val json = JSONObject(defaultMapping.toMap())
            return Braze.getInstance(context).deserializeContentCard(json)
        }

        private fun getRandomString(): String = UUID.randomUUID().toString()

        private fun getRandomBoolean(): Boolean = random.nextBoolean()

        // Get now plus some random delta a minute into the future
        private fun getNow(): Long = getNowPlusDelta(TimeUnit.MILLISECONDS, random.nextInt(60000).toLong())

        private fun getNowPlusDelta(deltaUnits: TimeUnit, delta: Long): Long = System.currentTimeMillis() + deltaUnits.toMillis(delta)

        /**
         * @return Pair of url to aspect ratio
         */
        private fun getRandomImageUrl(): Pair<String, Double> {
            return if (random.nextInt(100) < 40) {
                // Return a SUPER high resolution image
                val url = "${SUPER_HIGH_RESOLUTION_IMAGES.shuffled(random).first()}?q=${System.nanoTime()}"
                Pair(url, 1.0)
            } else {
                val height = random.nextInt(500) + 200
                val width = random.nextInt(500) + 200
                Pair("https://picsum.photos/seed/${System.nanoTime()}/$width/$height", width.toDouble() / height.toDouble())
            }
        }

        /**
         * Merges the content of a target map with another map
         */
        private fun MutableMap<String, Any>.mergeWith(another: Map<String, Any>) {
            for ((key, value) in another) {
                this[key] = value
            }
        }
    }
}
