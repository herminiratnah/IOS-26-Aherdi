package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSWebEngineView
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

data class BookItem(
    val id: String,
    val title: String,
    val author: String,
    val coverColor: Color,
    val category: String,
    val content: String
)

@Composable
fun BooksApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val airplaneMode by viewModel.airplaneMode.collectAsState()

    var activeTab by remember { mutableStateOf("Library") } // "Library" or "Online Store (Safari)"
    var readingBook by remember { mutableStateOf<BookItem?>(null) }
    var readerFontSize by remember { mutableFloatStateOf(16f) }
    var readerTheme by remember { mutableStateOf("Sepia") } // "Light", "Sepia", "Dark"

    val books = remember {
        listOf(
            BookItem(
                id = "1",
                title = "Alice's Adventures in Wonderland",
                author = "Lewis Carroll",
                coverColor = Color(0xFF1E88E5),
                category = "Classic Fiction",
                content = """
                    CHAPTER I. Down the Rabbit-Hole

                    Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice 'without pictures or conversation?'

                    So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.

                    There was nothing so VERY remarkable in that; nor did Alice think it so VERY much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!' (when she thought it over afterwards, it occurred to her that she ought to have wondered at this, but at the time it all seemed quite natural); but when the Rabbit actually TOOK A WATCH OUT OF ITS WAISTCOAT-POCKET, and looked at it, and then hurried on, Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket, or a watch to take out of it, and burning with curiosity, she ran across the field after it, and fortunately was just in time to see it pop down a large rabbit-hole under the hedge.
                """.trimIndent()
            ),
            BookItem(
                id = "2",
                title = "The Adventures of Sherlock Holmes",
                author = "Arthur Conan Doyle",
                coverColor = Color(0xFFD84315),
                category = "Mystery",
                content = """
                    I. A SCANDAL IN BOHEMIA

                    To Sherlock Holmes she is always THE woman. I have seldom heard him mention her under any other name. In his eyes she eclipses and predominates the whole of her sex. It was not that he felt any emotion akin to love for Irene Adler. All emotions, and that one particularly, were abhorrent to his cold, precise but admirably balanced mind.

                    He was, I take it, the most perfect reasoning and observing machine that the world has seen, but as a lover he would have placed himself in a false position. He never spoke of the softer passions, save with a gibe and a sneer.

                    One night—it was on the twentieth of March, 1888—I was returning from a journey to a patient, when my way led me through Baker Street. As I passed the well-remembered door, I was seized with a keen desire to see Holmes again, and to know how he was employing his extraordinary powers.
                """.trimIndent()
            ),
            BookItem(
                id = "3",
                title = "The Art of War",
                author = "Sun Tzu",
                coverColor = Color(0xFF2E7D32),
                category = "Philosophy",
                content = """
                    CHAPTER I. LAYING PLANS

                    Sun Tzu said: The art of war is of vital importance to the State. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.

                    The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field. These are: (1) The Moral Law; (2) Heaven; (3) Earth; (4) The Commander; (5) Method and discipline.

                    All warfare is based on deception. Hence, when able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away; when far away, we must make him believe we are near.
                """.trimIndent()
            ),
            BookItem(
                id = "4",
                title = "Pride and Prejudice",
                author = "Jane Austen",
                coverColor = Color(0xFF8E24AA),
                category = "Romance",
                content = """
                    CHAPTER I.

                    It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.

                    However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.

                    "My dear Mr. Bennet," said his lady to him one day, "have you heard that Netherfield Park is let at last?"
                """.trimIndent()
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (readingBook != null && readerTheme == "Dark") Color.Black else if (readingBook != null && readerTheme == "Sepia") Color(0xFFFBF0D9) else Color(0xFFF2F2F7))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                isBatteryCharging = isBatteryCharging,
                isWifiConnected = isWifiConnected,
                wifiSignalLevel = wifiSignalLevel,
                cellularBars = cellularBars,
                networkType = networkType,
                isAirplaneMode = airplaneMode,
                isDarkIcons = !(readingBook != null && readerTheme == "Dark")
            )

            if (readingBook != null) {
                // In-book Reader View
                val currentBook = readingBook!!
                val textColor = if (readerTheme == "Dark") Color.White else Color(0xFF2C2C2E)
                val bgColor = if (readerTheme == "Dark") Color.Black else if (readerTheme == "Sepia") Color(0xFFFBF0D9) else Color.White

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor)
                        .padding(horizontal = 20.dp)
                ) {
                    // Reader Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { readingBook = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = IOSBlue)
                        }

                        // Theme & Text Size
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { readerFontSize = (readerFontSize - 2f).coerceAtLeast(12f) }) {
                                Text("A-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IOSBlue)
                            }
                            IconButton(onClick = { readerFontSize = (readerFontSize + 2f).coerceAtMost(28f) }) {
                                Text("A+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IOSBlue)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                                    .clickable {
                                        readerTheme = when (readerTheme) {
                                            "Sepia" -> "Dark"
                                            "Dark" -> "Light"
                                            else -> "Sepia"
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(readerTheme, fontSize = 12.sp, color = IOSBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Book Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = currentBook.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = currentBook.author,
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentBook.content,
                            fontSize = readerFontSize.sp,
                            lineHeight = (readerFontSize * 1.5).sp,
                            fontFamily = FontFamily.Serif,
                            color = textColor
                        )
                    }
                }
            } else if (activeTab == "Online Store (Safari)") {
                IOSWebEngineView(
                    initialUrl = "https://www.gutenberg.org",
                    title = "Project Gutenberg Real-time Books",
                    modifier = Modifier.weight(1f),
                    showHeaderBar = true
                )
            } else {
                // Books Library Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Books",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Reading Now & Online Library",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE5E5EA))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (activeTab == "Library") IOSBlue else Color.Transparent)
                                .clickable { activeTab = "Library" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Library", color = if (activeTab == "Library") Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (activeTab == "Online Store (Safari)") IOSBlue else Color.Transparent)
                                .clickable { activeTab = "Online Store (Safari)" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = if (activeTab == "Online Store (Safari)") Color.White else Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online Books", color = if (activeTab == "Online Store (Safari)") Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Continue Reading", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    items(books) { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable { readingBook = book }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Book Spine / Cover
                            Box(
                                modifier = Modifier
                                    .width(55.dp)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(book.coverColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = book.author,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = book.category,
                                    fontSize = 11.sp,
                                    color = IOSBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = "Read",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }

            HomeBar(onGoHome = { viewModel.closeApp() }, isDark = false)
        }
    }
}
